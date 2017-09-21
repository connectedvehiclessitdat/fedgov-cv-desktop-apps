@echo off
SETLOCAL
java -cp fedgov-cv-desktop-apps-1.0.0-SNAPSHOT-jar-with-dependencies.jar gov.usdot.desktop.apps.provider.sdpc.MongoDbSdpcSenderProvider sdpc_int_sit_data_config2.json
ENDLOCAL
@echo on
