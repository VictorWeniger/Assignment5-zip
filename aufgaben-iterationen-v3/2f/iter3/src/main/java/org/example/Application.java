package org.example;

import org.example.api.ApiDocsController;
import org.example.api.ExportController;
import org.example.api.FrontendController;
import org.example.api.HealthController;
import org.example.api.ImportController;
import org.example.api.NlpController;
import org.example.api.ProtocolController;
import org.example.api.SwaggerController;
import org.example.api.TemplateController;
import org.example.config.AppConfig;
import org.example.db.MongoDatabaseHandler;
import org.example.model.Deputy;
import org.example.model.ExportTemplate;
import org.example.model.ProtocolDocument;
import org.example.model.ProtocolSession;
import org.example.model.Speech;
import org.example.model.SpeechVideo;
import org.example.service.BundestagProtocolDownloader;
import org.example.service.AgendaVideoImportService;
import org.example.service.DatabaseIndexInitializer;
import org.example.service.DeputyImageEnrichmentService;
import org.example.service.DeputyMasterDataImportService;
import org.example.service.ImportScheduler;
import org.example.service.MediaAssetDownloadService;
import org.example.service.NlpAnnotationImportService;
import org.example.service.NlpProcessingService;
import org.example.service.ProtocolImportService;
import org.example.service.XmlProtocolParser;
import org.example.service.nlp.NlpEngineSelector;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.rendering.template.JavalinFreemarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author
 * Victor Weniger
 */

/**
 * Application
 */
public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

/**
 * Method
 */
    public static void main(String[] args) {
        AppConfig config = AppConfig.fromEnvironment();
        LOGGER.info("MongoDB target database: {}", config.databaseConfig().databaseName());

        MongoDatabaseHandler<ProtocolDocument> protocolDatabase = new MongoDatabaseHandler<>(config.databaseConfig());
        MongoDatabaseHandler<ProtocolSession> sessionDatabase = new MongoDatabaseHandler<>(config.databaseConfig());
        MongoDatabaseHandler<Speech> speechDatabase = new MongoDatabaseHandler<>(config.databaseConfig());
        MongoDatabaseHandler<Deputy> deputyDatabase = new MongoDatabaseHandler<>(config.databaseConfig());
        MongoDatabaseHandler<SpeechVideo> speechVideoDatabase = new MongoDatabaseHandler<>(config.databaseConfig());
        MongoDatabaseHandler<ExportTemplate> templateDatabase = new MongoDatabaseHandler<>(config.databaseConfig());
        new DatabaseIndexInitializer(protocolDatabase.mongoClient(), config.databaseConfig().databaseName()).ensureIndexes();

        DeputyImageEnrichmentService deputyImageEnrichmentService = new DeputyImageEnrichmentService();
        MediaAssetDownloadService mediaAssetDownloadService =
                new MediaAssetDownloadService(config.downloadMediaAssets(), config.mediaDirectory());
        DeputyMasterDataImportService deputyMasterDataImportService = new DeputyMasterDataImportService();
        AgendaVideoImportService agendaVideoImportService = new AgendaVideoImportService(
                speechDatabase,
                sessionDatabase,
                speechVideoDatabase,
                mediaAssetDownloadService
        );
        ProtocolImportService importService = new ProtocolImportService(
                new BundestagProtocolDownloader(),
                protocolDatabase,
                sessionDatabase,
                speechDatabase,
                deputyDatabase,
                speechVideoDatabase,
                deputyImageEnrichmentService,
                mediaAssetDownloadService,
                deputyMasterDataImportService,
                agendaVideoImportService,
                new XmlProtocolParser()
        );
        NlpProcessingService nlpService = new NlpProcessingService(
                speechDatabase,
                speechVideoDatabase,
                NlpEngineSelector.select(config.nlpConfig())
        );
        NlpAnnotationImportService nlpImportService = new NlpAnnotationImportService(speechDatabase);

        ImportScheduler scheduler = new ImportScheduler();
        scheduler.schedule(importService, config.importInterval());

        Javalin app = Javalin.create(javalinConfig -> {
            javalinConfig.showJavalinBanner = false;
            javalinConfig.bundledPlugins.enableCors(cors -> cors.addRule(rule -> rule.anyHost()));
            javalinConfig.staticFiles.add("/public");
            javalinConfig.fileRenderer(new JavalinFreemarker());
        }).start(config.port());
        app.before(ctx -> {
            if (ctx.path().startsWith("/api")) {
                ctx.contentType(ContentType.JSON);
            }
        });
        app.exception(Exception.class, (e, ctx) -> {
            LOGGER.error("Unhandled server error", e);
            ctx.status(500).json(java.util.Map.of(
                    "error", "internal_server_error",
                    "message", String.valueOf(e.getMessage())
            ));
        });

        HealthController.register(app);
        ApiDocsController.register(app);
        SwaggerController.register(app);
        ImportController.register(app, importService, agendaVideoImportService);
        ProtocolController.register(
                app,
                protocolDatabase,
                sessionDatabase,
                speechDatabase,
                deputyDatabase,
                speechVideoDatabase,
                deputyImageEnrichmentService,
                nlpService
        );
        ExportController.register(app, speechDatabase, templateDatabase);
        TemplateController.register(app, templateDatabase);
        NlpController.register(app, nlpService, nlpImportService);
        FrontendController.register(app);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.close();
            protocolDatabase.close();
            sessionDatabase.close();
            speechDatabase.close();
            deputyDatabase.close();
            speechVideoDatabase.close();
            templateDatabase.close();
            app.stop();
        }));
    }
}
