package main;

import java.io.File;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;



//import javafx.application.Platform;

public class PlayMapMain{

	public static String apiKey= "AAPKe46630be6cda457b8acfde086c4ac5e9cx4j8Z1wqgZKXV4FSliSL8XAH6G5mWa2YIfuXj0tF513AaixG9p6g5HGf6_iEM-c";
	  
	public static void main(String[] args) {
		ArcGISRuntimeEnvironment.setInstallDirectory("C:/APJ-Client/ProgramFiles/app" + File.separator + "200.2.0");
		ArcGISRuntimeEnvironment.setLicense("runtimelite,1000,rud6034601507,none,TRB3LNBHPFK2NERL1114");
//		ArcGISRuntimeEnvironment.setApiKey("AAPKd92a8446a6b943179da8afc169500fde9w9_cPF6pyJfNOvUirylYLcWQ9H8LD27tDteXzjkK9WKzTKZbqrwHOuIi3IPm3yy");
		ArcGISRuntimeEnvironment.setApiKey(apiKey);
		ViewMap vm= new ViewMap();
		System.out.println("terminated");
		System.exit(0);
	}

}
