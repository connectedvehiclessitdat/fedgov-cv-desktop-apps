package gov.usdot.desktop.apps.provider;


public interface GeoPointProvider {

	public void init() throws InitializationException;
	public void dispose() throws InitializationException;
	public void start() throws InitializationException;
	public void stop() throws InitializationException;
	public void pause() throws InitializationException;
	public void resume() throws InitializationException;
	public boolean canPause();
	public boolean canStop();
	public void region(double nw_lat,double nw_lon,double se_lat,double se_lon);
	public void save(String configFilePath) throws InitializationException;
}
