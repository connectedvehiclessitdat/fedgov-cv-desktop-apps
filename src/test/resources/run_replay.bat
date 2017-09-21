@echo off
SETLOCAL
java -cp fedgov-cv-desktop-apps-1.0.0-SNAPSHOT-jar-with-dependencies.jar gov.usdot.desktop.apps.provider.sdpc.MongoDbSdpcSenderProvider sdpc_veh_sit_data_config.json
ENDLOCAL
@echo on
