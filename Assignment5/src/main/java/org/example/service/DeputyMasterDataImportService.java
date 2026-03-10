package org.example.service;

import org.example.db.DatabaseHandler;
import org.example.model.Deputy;
import org.example.model.DeputyInstitutionMembership;
import org.example.model.DeputyLegislativePeriod;
import org.example.model.ImageMetadata;
import org.example.model.ParliamentaryGroup;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Imports complete Bundestag deputy master data for one legislative period.
 */
public class DeputyMasterDataImportService {
    private static final URI MASTER_DATA_URI = URI.create("https://www.bundestag.de/resource/blob/472878/MdB-Stammdaten.zip");
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public ImportResult importLegislativePeriod(
            int legislativePeriod,
            DatabaseHandler<Deputy> deputyDatabase,
            DeputyImageEnrichmentService deputyImageEnrichmentService,
            MediaAssetDownloadService mediaAssetDownloadService
    ) {
        if (legislativePeriod <= 0) {
            throw new IllegalArgumentException("legislativePeriod must be > 0");
        }

        List<Deputy> imported = parseDeputiesForLegislativePeriod(legislativePeriod);
        int upserted = 0;
        int imagesAdded = 0;

        for (Deputy importedDeputy : imported) {
            Deputy merged = deputyDatabase.findById("deputies", importedDeputy.getId(), Deputy.class)
                    .map(existing -> mergeDeputy(existing, importedDeputy))
                    .orElse(importedDeputy);

            if (merged.getImages().isEmpty()) {
                deputyImageEnrichmentService.enrichWithProfileImage(merged);
                for (ImageMetadata image : merged.getImages()) {
                    mediaAssetDownloadService.downloadDeputyImage(image, merged.getId());
                    imagesAdded++;
                }
            }

            deputyDatabase.replaceById("deputies", merged.getId(), merged);
            upserted++;
        }

        return new ImportResult(legislativePeriod, imported.size(), upserted, imagesAdded);
    }

    private List<Deputy> parseDeputiesForLegislativePeriod(int legislativePeriod) {
        try {
            HttpRequest request = HttpRequest.newBuilder(MASTER_DATA_URI)
                    .header("User-Agent", "Assignment5DeputyImporter/1.0")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("HTTP " + response.statusCode() + " for " + MASTER_DATA_URI);
            }

            try (InputStream body = response.body();
                 ZipInputStream zip = new ZipInputStream(body, StandardCharsets.UTF_8)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (!entry.getName().toUpperCase().endsWith(".XML")) {
                        continue;
                    }

                    var factory = DocumentBuilderFactory.newInstance();
                    factory.setNamespaceAware(false);
                    factory.setExpandEntityReferences(false);
                    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                    factory.setXIncludeAware(false);
                    var builder = factory.newDocumentBuilder();
                    builder.setEntityResolver((publicId, systemId) -> new org.xml.sax.InputSource(new java.io.StringReader("")));
                    var document = builder.parse(zip);
                    NodeList nodes = document.getDocumentElement().getElementsByTagName("MDB");
                    List<Deputy> deputies = new ArrayList<>();
                    for (int i = 0; i < nodes.getLength(); i++) {
                        if (!(nodes.item(i) instanceof Element mdb)) {
                            continue;
                        }
                        Optional<Deputy> deputy = parseDeputyForPeriod(mdb, legislativePeriod);
                        deputy.ifPresent(deputies::add);
                    }
                    return deputies;
                }
            }
            return List.of();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not import Bundestag deputy master data: " + ex.getMessage(), ex);
        }
    }

    private Optional<Deputy> parseDeputyForPeriod(Element mdb, int legislativePeriod) {
        List<Element> periods = children(firstChild(mdb, "WAHLPERIODEN"), "WAHLPERIODE");
        DeputyLegislativePeriod matchingPeriod = null;
        for (Element period : periods) {
            int current = parseInt(textOf(period, "WP"));
            if (current == legislativePeriod) {
                matchingPeriod = parseLegislativePeriod(period, legislativePeriod);
                break;
            }
        }
        if (matchingPeriod == null) {
            return Optional.empty();
        }

        Deputy deputy = new Deputy();
        deputy.setId(textOf(mdb, "ID"));

        Element currentName = selectCurrentName(firstChild(mdb, "NAMEN"));
        deputy.setLastName(textOf(currentName, "NACHNAME"));
        deputy.setFirstName(textOf(currentName, "VORNAME"));
        deputy.setTitle(firstNonBlank(textOf(currentName, "AKAD_TITEL"), textOf(currentName, "ANREDE_TITEL")));

        Element bio = firstChild(mdb, "BIOGRAFISCHE_ANGABEN");
        deputy.setBirthDate(parseDate(textOf(bio, "GEBURTSDATUM")));
        deputy.setBirthPlace(textOf(bio, "GEBURTSORT"));
        deputy.setBirthCountry(textOf(bio, "GEBURTSLAND"));
        deputy.setDeathDate(parseDate(textOf(bio, "STERBEDATUM")));
        deputy.setGender(textOf(bio, "GESCHLECHT"));
        deputy.setMaritalStatus(textOf(bio, "FAMILIENSTAND"));
        deputy.setReligion(textOf(bio, "RELIGION"));
        deputy.setProfession(textOf(bio, "BERUF"));
        deputy.setPartyShort(textOf(bio, "PARTEI_KURZ"));
        deputy.setVitaShort(textOf(bio, "VITA_KURZ"));
        deputy.setPublicationRequiredInfo(textOf(bio, "VEROEFFENTLICHUNGSPFLICHTIGES"));

        ParliamentaryGroup group = parliamentaryGroupFromPeriod(matchingPeriod, deputy.getPartyShort());
        deputy.setParliamentaryGroup(group);
        deputy.getLegislativePeriods().add(matchingPeriod);
        return Optional.of(deputy);
    }

    private Deputy mergeDeputy(Deputy existing, Deputy imported) {
        Deputy merged = existing == null ? new Deputy() : existing;
        merged.setId(firstNonBlank(existing == null ? null : existing.getId(), imported.getId()));
        merged.setFirstName(firstNonBlank(existing == null ? null : existing.getFirstName(), imported.getFirstName()));
        merged.setLastName(firstNonBlank(existing == null ? null : existing.getLastName(), imported.getLastName()));
        merged.setTitle(firstNonBlank(imported.getTitle(), existing == null ? null : existing.getTitle()));
        merged.setBirthDate(firstNonNull(imported.getBirthDate(), existing == null ? null : existing.getBirthDate()));
        merged.setBirthPlace(firstNonBlank(imported.getBirthPlace(), existing == null ? null : existing.getBirthPlace()));
        merged.setBirthCountry(firstNonBlank(imported.getBirthCountry(), existing == null ? null : existing.getBirthCountry()));
        merged.setDeathDate(firstNonNull(imported.getDeathDate(), existing == null ? null : existing.getDeathDate()));
        merged.setGender(firstNonBlank(imported.getGender(), existing == null ? null : existing.getGender()));
        merged.setMaritalStatus(firstNonBlank(imported.getMaritalStatus(), existing == null ? null : existing.getMaritalStatus()));
        merged.setReligion(firstNonBlank(imported.getReligion(), existing == null ? null : existing.getReligion()));
        merged.setProfession(firstNonBlank(imported.getProfession(), existing == null ? null : existing.getProfession()));
        merged.setPartyShort(firstNonBlank(imported.getPartyShort(), existing == null ? null : existing.getPartyShort()));
        merged.setVitaShort(firstNonBlank(imported.getVitaShort(), existing == null ? null : existing.getVitaShort()));
        merged.setPublicationRequiredInfo(firstNonBlank(imported.getPublicationRequiredInfo(), existing == null ? null : existing.getPublicationRequiredInfo()));
        merged.setParliamentaryGroup(imported.getParliamentaryGroup() != null ? imported.getParliamentaryGroup() : existing.getParliamentaryGroup());

        Set<Integer> knownPeriods = new LinkedHashSet<>();
        for (DeputyLegislativePeriod period : merged.getLegislativePeriods()) {
            knownPeriods.add(period.getLegislativePeriod());
        }
        for (DeputyLegislativePeriod period : imported.getLegislativePeriods()) {
            if (knownPeriods.add(period.getLegislativePeriod())) {
                merged.getLegislativePeriods().add(period);
            }
        }
        if (merged.getImages().isEmpty() && existing != null) {
            merged.getImages().addAll(existing.getImages());
        }
        return merged;
    }

    private ParliamentaryGroup parliamentaryGroupFromPeriod(DeputyLegislativePeriod period, String partyShort) {
        ParliamentaryGroup group = new ParliamentaryGroup();
        String label = null;
        for (DeputyInstitutionMembership membership : period.getInstitutions()) {
            if (membership.getTypeLabel() != null && membership.getTypeLabel().toLowerCase().contains("fraktion")) {
                label = membership.getLabel();
                break;
            }
        }
        label = firstNonBlank(label, partyShort);
        group.setId(toFactionId(label));
        group.setShortName(label);
        group.setDisplayName(label);
        return group;
    }

    private DeputyLegislativePeriod parseLegislativePeriod(Element period, int legislativePeriod) {
        DeputyLegislativePeriod out = new DeputyLegislativePeriod();
        out.setLegislativePeriod(legislativePeriod);
        out.setMemberFrom(parseDate(textOf(period, "MDBWP_VON")));
        out.setMemberTo(parseDate(textOf(period, "MDBWP_BIS")));
        out.setConstituencyNumber(textOf(period, "WKR_NUMMER"));
        out.setConstituencyName(textOf(period, "WKR_NAME"));
        out.setConstituencyState(textOf(period, "WKR_LAND"));
        out.setListName(textOf(period, "LISTE"));
        out.setMandateType(textOf(period, "MANDATSART"));

        Element institutions = firstChild(period, "INSTITUTIONEN");
        for (Element institution : children(institutions, "INSTITUTION")) {
            DeputyInstitutionMembership membership = new DeputyInstitutionMembership();
            membership.setTypeLabel(textOf(institution, "INSART_LANG"));
            membership.setLabel(textOf(institution, "INS_LANG"));
            membership.setMemberFrom(parseDate(textOf(institution, "MDBINS_VON")));
            membership.setMemberTo(parseDate(textOf(institution, "MDBINS_BIS")));
            membership.setFunctionLabel(textOf(institution, "FKT_LANG"));
            membership.setFunctionFrom(parseDate(textOf(institution, "FKTINS_VON")));
            membership.setFunctionTo(parseDate(textOf(institution, "FKTINS_BIS")));
            out.getInstitutions().add(membership);
        }
        return out;
    }

    private Element selectCurrentName(Element names) {
        List<Element> values = children(names, "NAME");
        return values.stream()
                .max(Comparator.comparing((Element el) -> parseDate(textOf(el, "HISTORIE_VON")), Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);
    }

    private List<Element> children(Element parent, String tagName) {
        List<Element> out = new ArrayList<>();
        if (parent == null) {
            return out;
        }
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element child && tagName.equals(child.getTagName())) {
                out.add(child);
            }
        }
        return out;
    }

    private Element firstChild(Element parent, String tagName) {
        if (parent == null) {
            return null;
        }
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element child && tagName.equals(child.getTagName())) {
                return child;
            }
        }
        return null;
    }

    private String textOf(Element parent, String tagName) {
        Element child = firstChild(parent, tagName);
        if (child == null || child.getTextContent() == null) {
            return null;
        }
        String value = child.getTextContent().trim();
        return value.isBlank() ? null : value;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value, DMY);
        } catch (Exception ignored) {
            return null;
        }
    }

    private int parseInt(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String toFactionId(String label) {
        if (label == null || label.isBlank()) {
            return "independent";
        }
        String normalized = label.toLowerCase()
                .replace("fraktion der ", "")
                .replace("gruppe der ", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "independent" : normalized;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static <T> T firstNonNull(T preferred, T fallback) {
        return preferred != null ? preferred : fallback;
    }

    public record ImportResult(int legislativePeriod, int matchedDeputies, int upsertedDeputies, int imageAttempts) {
    }
}
