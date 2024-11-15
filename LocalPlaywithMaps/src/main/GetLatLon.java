package main;

import java.util.List;
import java.util.concurrent.ExecutionException;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.LocatorTask;

public class GetLatLon {
	
	  private GeocodeParameters geocodeParameters;
	  private LocatorTask locatorTask;
	  
	  public GetLatLon() {
		  createLocatorTaskAndDefaultParameters();
	  }
	  
	  public void performGeocode(String address) {
		    ListenableFuture<List<GeocodeResult>> geocodeResults = locatorTask.geocodeAsync(address, geocodeParameters);

		    geocodeResults.addDoneListener(() -> {
//		      try {
		        List<GeocodeResult> geocodes;
				try {
					geocodes = geocodeResults.get();

			        if (geocodes.size() > 0) {
			          GeocodeResult result = geocodes.get(0);
			          System.out.println("found");
			         Point pt= result.getDisplayLocation();
			         System.out.println("x= " + pt.getX() + " y= "+ pt.getY() + " ref = " + pt.getSpatialReference().getWKText());
			        } else {
			        	System.out.println("No results found.");
			        }
				} catch (InterruptedException | ExecutionException e) {
			    	  System.out.println("Error getting result..");
					e.printStackTrace();
				}
		    });
		  }
	  

	  private void createLocatorTaskAndDefaultParameters() {
		    locatorTask = new LocatorTask("https://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer");

		    geocodeParameters = new GeocodeParameters();
		    geocodeParameters.getResultAttributeNames().add("*");
		    geocodeParameters.setMaxResults(1);
//		    geocodeParameters.setOutputSpatialReference("");
		  }
	  
	  
}
