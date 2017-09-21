@echo off
SETLOCAL
REM Uncomment the line below to enable design mode
REM set DESIGN=-d
start /B javaw -cp fedgov-cv-desktop-apps-1.0.0-SNAPSHOT-jar-with-dependencies.jar gov.usdot.desktop.apps.gui.map.GeoPointsMapper %DESIGN% -g replay_veh_sit_data_gui_config.json
start /B javaw -cp fedgov-cv-desktop-apps-1.0.0-SNAPSHOT-jar-with-dependencies.jar gov.usdot.desktop.apps.gui.map.GeoPointsMapper %DESIGN% -g subscriber_1_gui_config.json
ENDLOCAL
@echo on
