package maps;

import java.awt.Window;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingUtilities;

import com.esri.arcgisruntime.concurrent.ListenableFuture;

import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.GeometryType;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.FeatureCollectionLayer;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.BasemapStyle;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.IdentifyGraphicsOverlayResult;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.mapping.view.geometryeditor.GeometryEditor;
import com.esri.arcgisruntime.mapping.view.geometryeditor.VertexTool;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.symbology.Renderer;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;
import com.fpi.apjibe.mapping.BuildMap2.MapType;
import com.fpi.apjibe.server.jibemodel.expansion2.data_gathering.roads.RoadHandler;
import com.fpi.apjibe.server.jibemodel.expansion2.graph.Edge;
import com.fpi.apjibe.server.jibemodel.expansion2.graph.Road;
import com.fpi.apjibe.server.jibemodel.expansion2.graph.Road.RoadType;
import com.fpi.apjibe.server.jibemodel.expansion2.util.DistanceCalculator;
import com.fpi.apjibe.server.jibemodel.expansion2.util.WriteToGeoJSON;

import com.fpi.apjibe.mapping.LayerID;
import com.fpi.apjibe.mapping.MapStyle2;
import com.fpi.apjibe.mapping.ReadGeoJSON;
import com.fpi.apjibe.mapping.TempFeature;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import main.PlayMapMain;

/**
 *Class to hold road map for editing
 *
 */
public class BuildRoadMap extends JFXPanel {

	/**
	 * serial id
	 */
	private static final long serialVersionUID = -7365984276244740882L;

	protected MapView mapView;

	protected MapStyle2 mapStyle;

	private StackPane rootControl;

	private ArcGISMap map;

	private Scene scene;

	private boolean loaded;

	private GeometryEditor geometryEditor;
	
	private VertexTool vertexTool;
	
	private Callout callout;

	private Geometry newGeometry;
	
	 private GraphicsOverlay newOverlay, definedRoadsOverlay, editOverlay ;

	// Key = LayerID.toString, Graphic Overlay
	private Map<String, GraphicsOverlay> graphicOverlayMap;
	
	/** 
	 * NOTE: Graphics and roads need to be kept in sync at all times and both maps need to be updated with
	 * every change
	 */
	/** map roads to graphics on layer */
	private HashMap<Road,GraphAndLayer> roadToGraphMap;

	/** map graphics to roads for reverse lookup graphics on layer */
	private HashMap<Graphic,Road> graphToRoadMap;
	
	private SimpleRenderer spliceRenderer;

	  private Graphic selectedGraphic;
	  
	  private String path;
	  
	  private SpatialReference sr;
	  
	  SimpleLineSymbol dashStyle,newStyle;
	  
	/**
	 * Creates an Esri Map as a panel from an Esri .json file. This constructor is
	 * used for the interactive Census Node map.
	 * 
	 * @param resultFile

	 */
	public BuildRoadMap(String resultFile,String path) {
		super();
		this.path=path;
		this.loaded = false;
		sr= SpatialReferences.getWgs84();
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				dashStyle = new SimpleLineSymbol(SimpleLineSymbol.Style.DASH, Color.BLACK, 2.0f);
				newStyle = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLUE, 2.0f);
				
				roadToGraphMap = new HashMap<Road,GraphAndLayer>();
				graphToRoadMap = new HashMap<Graphic,Road> ();
				mapView = new MapView();

				rootControl = new StackPane();
				scene = new Scene(rootControl);
				graphicOverlayMap = new HashMap<>();
				setScene(scene);
				setSpliceRenderer();
				// read map from File
				// Expansion Map here
				map = new ArcGISMap(BasemapStyle.ARCGIS_STREETS);

			
//				map = new ArcGISMap(BasemapStyle.ARCGIS_IMAGERY);
				String fullResultName = path+File.separator + resultFile;
				File jsonFile = new File(fullResultName);
				try {
					if (jsonFile.exists()) {
						ReadGeoJSON readJson = new ReadGeoJSON(fullResultName, sr);
						Map<String, List<TempFeature>> mapOfFeatures = readJson.readGeoJson();
						mapStyle = new MapStyle2(mapOfFeatures);
						setStylesAndAddToMap(mapOfFeatures);

						List<Geometry> envelopeBuilder = new LinkedList<Geometry>();
						List<TempFeature> nodeFeatures = mapOfFeatures.get(LayerID.NODE_POINT.toString());
						List<TempFeature> facilityFeatures = mapOfFeatures.get(LayerID.FACILITY.toString());
						List<TempFeature> spliceFeatures = mapOfFeatures.get(LayerID.SPLICE_POINT.toString());
						for (TempFeature f : nodeFeatures) {
							envelopeBuilder.add(f.getGeometry());
						}
						if (facilityFeatures == null)
							facilityFeatures = new LinkedList<TempFeature>();
						for (TempFeature f : facilityFeatures) {
							envelopeBuilder.add(GeometryEngine.buffer(f.getGeometry(), .001));
						}
						if (spliceFeatures == null)
							spliceFeatures = new LinkedList<TempFeature>();
						for (TempFeature f : spliceFeatures) {
							envelopeBuilder.add(f.getGeometry());
						}

						List<TempFeature> roads = mapOfFeatures.get(LayerID.ROADS.toString());
						if (roads != null) {
							for (TempFeature f : roads) {
								envelopeBuilder.add(f.getGeometry().getExtent());
							}
						}

						definedRoadsOverlay= new GraphicsOverlay();
						Envelope env = GeometryEngine.combineExtents(envelopeBuilder);
						
						Viewpoint vp = new Viewpoint(env);
						map.setInitialViewpoint(vp);
						// read defined roads from file or query envelop for roads
						String fullRoadName = path+File.separator + "areaRoads.geojson";
						File roadFile = new File(fullRoadName);
						if (roadFile.exists()) {
							ReadGeoJSON readJsonRoads = new ReadGeoJSON(fullRoadName, sr);
							Map<String, List<TempFeature>> mapOfRoadFeatures = readJsonRoads.readGeoJson();
							mapStyle = new MapStyle2(mapOfRoadFeatures);

							List<TempFeature> areaRoads = mapOfRoadFeatures.get(LayerID.ROADS.toString());
							if (areaRoads != null) {
								for (TempFeature f : areaRoads) {
									Map<String,Object> attr=  f.getAttributes();
									String name = attr!=null &&  attr.get("NAME")!=null?(String)attr.get("NAME"):"";
	
									Road r  = new Road(f.getGeometry(),name, RoadType.LOCAL,attr);
									if (!roadToGraphMap.containsKey(r)) {
										Graphic roadG = new Graphic(f.getGeometry(), attr, dashStyle);
										roadToGraphMap.put(r,new GraphAndLayer(roadG, definedRoadsOverlay));
										graphToRoadMap.put(roadG, r);
										definedRoadsOverlay.getGraphics().add(roadG);
									}
								}
							}
						} else {
							RoadHandler roadHandler = new RoadHandler(env, sr, PlayMapMain.apiKey, null);
							List<Road> boundedRoads = roadHandler.gatherRoads();
							if (boundedRoads != null && !boundedRoads.isEmpty()) {
								for (Road r : boundedRoads) {	
									if (!roadToGraphMap.containsKey(r)) {
										Graphic roadG = new Graphic(r.getLine(), r.getAttributes(), dashStyle);
										roadToGraphMap.put(r,new GraphAndLayer(roadG, definedRoadsOverlay));
										graphToRoadMap.put(roadG, r);
										definedRoadsOverlay.getGraphics().add(roadG);
										}
								}
							}
						}


					}
				} catch (Exception e) {
					// do pop up
					Alert al = new Alert(AlertType.WARNING, "Error Loading map due to " + e.getMessage(),
							ButtonType.CLOSE);
					al.show();
					e.printStackTrace();
				}
				map.loadAsync();
				map.addDoneLoadingListener(() -> {
					if (map.getLoadStatus() == LoadStatus.LOADED) {

						try {
							mapStyle = new MapStyle2(map);
						} catch (Exception e) {
							throw new RuntimeException(e.getMessage());
						}
						addControlsToMap();
						setExpansionVisible();
						loaded = true;
					}
				});


			    mapView.getGraphicsOverlays().add(definedRoadsOverlay);
//			    graphicsOverlay = new GraphicsOverlay();
			    newOverlay = new GraphicsOverlay();
			    mapView.getGraphicsOverlays().add(newOverlay);
			    // set graphic editor to new layer
				editOverlay=newOverlay;
			    geometryEditor = new GeometryEditor();
			    mapView.setGeometryEditor(geometryEditor);
			    // create vertex tool and set as default
			    vertexTool = new VertexTool();		
			    geometryEditor.setTool(vertexTool);
			    ContextMenu snapMenu = rClickMenu();		    
				// TODO callout stuff here
				callout = mapView.getCallout();
				callout.setAutoAdjustWidth(true);
				callout.setMouseTransparent(true);
				TextArea textArea = new TextArea();
				textArea.setContextMenu(snapMenu);
				rootControl.getChildren().add(textArea);
				mapView.setOnMouseClicked(e -> {
					callout.dismiss();
			          Point2D mapViewPoint = new Point2D(e.getX(), e.getY());
			          // create a map point from a point
			          Point mapPoint = mapView.screenToLocation(mapViewPoint);
				      if (e.getButton() == MouseButton.PRIMARY && e.isStillSincePress()) {
				          editOverlay.clearSelection();
				          // get graphics near the clicked location
				          ListenableFuture<IdentifyGraphicsOverlayResult> identifyGraphics =
				            mapView.identifyGraphicsOverlayAsync(editOverlay, mapViewPoint, 10, false);

				          identifyGraphics.addDoneListener(() -> {
				            try {
					            if (!geometryEditor.isStarted()) {
					                if (!identifyGraphics.get().getGraphics().isEmpty()) {
						                // store the selected graphic so that it can be retained if edits are discarded
						                selectedGraphic = identifyGraphics.get().getGraphics().get(0);
						                selectedGraphic.setSelected(true);
						                // hide the selected graphic and start an editing session with a copy of it
//						                geometryEditor.start(selectedGraphic.getGeometry());
//						                selectedGraphic.setVisible(false);
					                } else {
					                	selectedGraphic= null;
						            	geometryEditor.start(GeometryType.POLYLINE);
					                }
						        }		 

				            } catch (Exception e1 ) {}
				      	});
				      }  else if (e.getButton() == MouseButton.SECONDARY) {

				    	  if (geometryEditor.isStarted()) {
//				    		  newGeometry = geometryEditor.stop();
			    			  snapMenu.show(textArea,e.getX(), e.getY());
				    	  } else {				    		  

					          // get graphics near the clicked location
					          ListenableFuture<IdentifyGraphicsOverlayResult> identifyGraphics =
					            mapView.identifyGraphicsOverlayAsync(editOverlay, mapViewPoint, 10, false);

					          identifyGraphics.addDoneListener(() -> {
						           try {
						                if (!identifyGraphics.get().getGraphics().isEmpty()) {
							                selectedGraphic = identifyGraphics.get().getGraphics().get(0);
							                String info = formatRoadAttr(selectedGraphic.getAttributes());
						                	
											callout.setTitle("Road information");
											// show the callout where the user clicked
											callout.setDetail(info);//
											callout.showCalloutAt(mapPoint);
						                }
						            } catch (Exception e1 ) {}
					            });
				    	  }
				      }			
				});

				mapView.setOnKeyPressed(e -> {
					if (e.isControlDown() && e.getCode()== KeyCode.Z) {
						if (geometryEditor.isStarted()) {
							geometryEditor.undo();
						}
					} else if (e.isControlDown() && e.getCode()== KeyCode.Y) {
						if (geometryEditor.isStarted()) {
							geometryEditor.redo();
						}
					}
				});
				mapView.setMap(map);
				rootControl.getChildren().add(mapView);
			}
		});
	}
	 
	  /**
	   * Creates a new graphic based on the geometry type used by the geometry editor, and adds to the graphics overlay.
	   */
	  private void deleteSelectedGraphic(ActionEvent e) {
	    // get the geometry from the geometry editor and create a new graphic for it

	    if (selectedGraphic !=null) {
	    	editOverlay.getGraphics().remove(selectedGraphic);
	    	Road r = graphToRoadMap.get(selectedGraphic);
	    	graphToRoadMap.remove(selectedGraphic);
	    	roadToGraphMap.remove(r);
	    	selectedGraphic=null;
	    }
	  }
	  /**
	   * Edit the selected graphic with geometry editor
	   */
	  private void editSelectedGraphic(ActionEvent e) {
	    // get the geometry from the geometry editor and create a new graphic for it
	    if (selectedGraphic !=null) {
            geometryEditor.start(selectedGraphic.getGeometry());
            selectedGraphic.setVisible(false);
	    }
	  }
	  /**
	   * Save all existing/edited and new roads
	   * @param e
	   */
	  private void saveAllRoads(ActionEvent e) {
		  	String filePath= path+File.separator+"areaRoads.geojson";
		  	WriteToGeoJSON writeRoads= new WriteToGeoJSON(filePath, sr,true);
		  	// write all the up to date roads 
		  	for (Road r: roadToGraphMap.keySet()) {
		  		writeRoads.addRoadFeature(r);
		  	}
			writeRoads.close();
			dispose();
			Window window = SwingUtilities.getWindowAncestor(this);
			window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSED));
	  }
	  private void cancel(ActionEvent e) {
		  	dispose();
			Window window = SwingUtilities.getWindowAncestor(this);
			window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSED));
	  }
	  private enum SnapperAction {
		  START,END,BOTH,NONE 
	  }
	  

	  private void snapper(SnapperAction action) {
		  
		  newGeometry = geometryEditor.stop();
		  Map<String,Object> attr = new HashMap<String,Object>();
		  SimpleLineSymbol styleForNew = newStyle;      
		   // if selected graphic remove from layer and maps and save road attribures
		    if (selectedGraphic !=null) {		    	
		    	attr = selectedGraphic.getAttributes();
		    	editOverlay.getGraphics().remove(selectedGraphic);
		    	Road selectedRoad = graphToRoadMap.get(selectedGraphic);
		    	roadToGraphMap.remove(selectedRoad);
		    	selectedGraphic=null;
		    	styleForNew=dashStyle;
		    } 
		    //create graphic and road 
	  		Graphic addedGraphic = new Graphic(newGeometry,attr,styleForNew);
	  		Geometry rg= GeometryEngine.project(addedGraphic.getGeometry(), sr);
			Road addedRoad = new Road(rg,"AddedRoad",RoadType.LOCAL,addedGraphic.getAttributes());
		    //find and update all intersecting roads and respective graphs on layer
			// also add crossing points to new road
			List<Road> allRoads= new LinkedList<Road>();
			allRoads.addAll(roadToGraphMap.keySet());
		    for (Road existingRoad: allRoads) {
		    	// if new road intersects with existing road updated existing road graphic and layer
		    	if (GeometryEngine.intersects(existingRoad.getLine(),addedRoad.getLine())) {
		    		GraphAndLayer existingEntry =  roadToGraphMap.get(existingRoad);
		    		GraphicsOverlay existingLayer = existingEntry.layer;
					existingLayer.getGraphics().remove(existingEntry.graph);
		    		roadToGraphMap.remove(existingRoad);
		    		graphToRoadMap.remove(existingEntry.graph);
		    		// create intersection points in both roads
		    		existingRoad.createIntersection(addedRoad);
		    		//reinstall updated existing road in layer
	    			SimpleLineSymbol style = existingLayer.equals(definedRoadsOverlay)?dashStyle:newStyle;   			
					Graphic roadG = new Graphic(existingRoad.getLine(), existingRoad.getAttributes(), style);
					roadToGraphMap.put(existingRoad,new GraphAndLayer(roadG, existingLayer));
					graphToRoadMap.put(roadG, existingRoad);
					existingLayer.getGraphics().add(roadG);
		    	}
		    }
	   		if (action == SnapperAction.NONE) {  
			    // create and add new graphics and road
			    addedGraphic = new Graphic(addedRoad.getLine(), addedRoad.getAttributes(), newStyle);
				roadToGraphMap.put(addedRoad,new GraphAndLayer(addedGraphic, newOverlay));
				graphToRoadMap.put(addedGraphic, addedRoad);
			    editOverlay.getGraphics().add(addedGraphic);
	   		} else {
	   			// snap start if both or start option
	   			ArrayList<Point> listOfPoints = new ArrayList<Point>();
				for (Point pt : addedRoad.getLine().getParts().getPartsAsPoints()) {
					listOfPoints.add(pt);
				}
	   			if (action == SnapperAction.BOTH || action == SnapperAction.START) {
	   				Point startP = listOfPoints.get(0);
	   				// Find closed road and closest point on the road
	   				double minDistance = Double.MAX_VALUE;
	   				Road minRoad = null;
	   				// find closest road
	   				for (Road r: roadToGraphMap.keySet()) {
	   					double distance = r.distanceToRoad(startP);
	   					// get the closest edge first, then create vertex!!!!!!!!!
	   					if (distance <= minDistance) {
	   						minDistance = distance;
	   	   						minRoad = r;
	   					}
	   				} // for loop over roads
	   				//find first point
	   				Point minPoint = minRoad.ClosestPointInRoad(startP);
	   				// update first point
	   				listOfPoints.set(0,minPoint);
	   				// remove road, place  intersection point on road and update road 
		    		GraphAndLayer existingEntry =  roadToGraphMap.get(minRoad);
		    		GraphicsOverlay existingLayer = existingEntry.layer;
					existingLayer.getGraphics().remove(existingEntry.graph);
		    		roadToGraphMap.remove(minRoad);
		    		graphToRoadMap.remove(existingEntry.graph);
		    		// create intersection points in both roads
		    		minRoad.addPointInRoad(minPoint);
		    		//reinstall updated existing road in layer
	    			SimpleLineSymbol style = existingLayer.equals(definedRoadsOverlay)?dashStyle:newStyle;   			
					Graphic roadG = new Graphic(minRoad.getLine(), minRoad.getAttributes(), style);
					roadToGraphMap.put(minRoad,new GraphAndLayer(roadG, existingLayer));
					graphToRoadMap.put(roadG, minRoad);
					existingLayer.getGraphics().add(roadG);	   				
	   			}
	   			// snap end if both or end option
	   			if (action == SnapperAction.BOTH || action == SnapperAction.END) {
	   				int lastIndex= listOfPoints.size()-1;
	 				Point endP = listOfPoints.get(lastIndex);
	   				// Find closed road and closest point on the road
	   				double minDistance = Double.MAX_VALUE;
	   				Road minRoad = null;
	   				// find closest road
	   				for (Road r: roadToGraphMap.keySet()) {
	   					double distance = r.distanceToRoad(endP);
	   					// get the closest edge first, then create vertex!!!!!!!!!
	   					if (distance <= minDistance) {
	   						minDistance = distance;
	   	   						minRoad = r;
	   					}
	   				} // for loop over roads
	   				//find first point
	   				Point minPoint = minRoad.ClosestPointInRoad(endP);
	   				// update first point
	   				listOfPoints.set(lastIndex,minPoint);
	   				// remove road, place  intersection point on road and update road 
		    		GraphAndLayer existingEntry =  roadToGraphMap.get(minRoad);
		    		GraphicsOverlay existingLayer = existingEntry.layer;
					existingLayer.getGraphics().remove(existingEntry.graph);
		    		roadToGraphMap.remove(minRoad);
		    		graphToRoadMap.remove(existingEntry.graph);
		    		// create intersection points in both roads
		    		minRoad.addPointInRoad(minPoint);
		    		//reinstall updated existing road in layer
	    			SimpleLineSymbol style = existingLayer.equals(definedRoadsOverlay)?dashStyle:newStyle;   			
					Graphic roadG = new Graphic(minRoad.getLine(), minRoad.getAttributes(), style);
					roadToGraphMap.put(minRoad,new GraphAndLayer(roadG, existingLayer));
					graphToRoadMap.put(roadG, minRoad);
					existingLayer.getGraphics().add(roadG);	   		
	   			}
				Polyline line = new Polyline(new PointCollection(listOfPoints, sr));
				addedRoad.setLine(line);
			    // create and add new graphics and road
			    addedGraphic = new Graphic(line, addedRoad.getAttributes(), newStyle);
				roadToGraphMap.put(addedRoad,new GraphAndLayer(addedGraphic, newOverlay));
				graphToRoadMap.put(addedGraphic, addedRoad);
			    editOverlay.getGraphics().add(addedGraphic);				
	   		}
	  }
	  
		private ContextMenu rClickMenu() {
			ContextMenu contextMenu = new ContextMenu();

	        // Create menu items with actions
	        MenuItem startItem = new MenuItem("Snap start");
	        startItem.setOnAction(e -> snapper(SnapperAction.START));

	        MenuItem endItem = new MenuItem("Snap end");
	        endItem.setOnAction(e ->snapper(SnapperAction.END));

	        MenuItem bothItem = new MenuItem("Snap both");
	        bothItem.setOnAction(e -> snapper(SnapperAction.BOTH));
	        
	        MenuItem noneItem = new MenuItem("Snap none");
	        noneItem.setOnAction(e -> snapper(SnapperAction.NONE));
	        contextMenu.getItems().addAll(startItem, endItem, bothItem,noneItem);
	        return contextMenu;
		}
		
	  
		private void setStylesAndAddToMap(Map<String, List<TempFeature>> mapOfFeatures) {

		for (var entry : mapOfFeatures.entrySet()) {
			// all layer names are toString of the LayerID enum
			String layerName = entry.getKey();
			List<TempFeature> features = entry.getValue();

			GraphicsOverlay baseFeatureOverlay = new GraphicsOverlay();

			// Add all features to graphic layer
			for (TempFeature feat : features) {
				Graphic g = new Graphic(feat.getGeometry(), feat.getAttributes());
				baseFeatureOverlay.getGraphics().add(g);
			}

			graphicOverlayMap.put(layerName, baseFeatureOverlay);
			if (layerName.equals(LayerID.SPLICE_POINT.toString())) {
				baseFeatureOverlay.setRenderer(spliceRenderer);
			} else {
				// Style the layer of graphics based on their type
				Renderer r = this.mapStyle.getRenderer(layerName, MapType.CENSUS);
				baseFeatureOverlay.setRenderer(r);
			}
			this.mapView.getGraphicsOverlays().add(baseFeatureOverlay);

		}

//		this.mapView.setOpaqueInsets(new Insets(20, 20, 20, 20));

	}

	/**
	 * Sets the default facility renderer
	 */
	public void setSpliceRenderer() {

		URL img = this.getClass().getClassLoader().getResource("CleanPlus.png");
		Image enabled = new Image(img.toExternalForm());

		PictureMarkerSymbol spliceSymbol = new PictureMarkerSymbol(enabled);
		spliceSymbol.setHeight(20);
		spliceSymbol.setWidth(20);
		SimpleRenderer sr = new SimpleRenderer(spliceSymbol);
		this.spliceRenderer = sr;

	}



	private void addControlsToMap() {
		BackgroundFill background_fill = new BackgroundFill(Color.WHITESMOKE, new CornerRadii(5), Insets.EMPTY);
		// base map selextion box
		HBox viewBox = new HBox();
		Label viewLabel = new Label("Basemap View:");
		ToggleGroup mapToggle = new ToggleGroup();
		RadioButton streets = new RadioButton("Streets");
		streets.setToggleGroup(mapToggle);
		streets.setSelected(true);
		RadioButton sat = new RadioButton("Satellite");
		sat.setToggleGroup(mapToggle);

		mapToggle.selectedToggleProperty().addListener((observable, oldVal, newVal) -> {
			RadioButton rb = (RadioButton) mapToggle.getSelectedToggle();
			String selectionMode = rb.getText();
			if (selectionMode.equals("Streets")) {
				this.map.setBasemap(new Basemap(BasemapStyle.ARCGIS_STREETS));
				streets.setSelected(true);
				sat.setSelected(false);
			} else if (selectionMode.equals("Satellite")) {
				this.map.setBasemap(new Basemap(BasemapStyle.ARCGIS_IMAGERY));
				streets.setSelected(false);
				sat.setSelected(true);
			}

		});
//		viewBox.setBackground(new Background(background_fill));
//		viewBox.setMaxWidth(250);
//		viewBox.setMaxHeight(30);
		viewBox.setSpacing(10);
		viewBox.getChildren().addAll(viewLabel, streets, sat);
//		viewBox.setPadding(new Insets(10, 10, 10, 10));

		// street selection box 
		HBox streetBox = new HBox();
		Label streetLabel = new Label("Street layer:");
		ToggleGroup streetToggle = new ToggleGroup();
		RadioButton esistingStreets = new RadioButton("Defined streets");
		esistingStreets.setToggleGroup(streetToggle);
		RadioButton newStreets = new RadioButton("New streets");
		newStreets.setToggleGroup(streetToggle);
		newStreets.setSelected(true);

		streetToggle.selectedToggleProperty().addListener((observable, oldVal, newVal) -> {
			RadioButton rb = (RadioButton) streetToggle.getSelectedToggle();
			String selectionMode = rb.getText();
			if (selectionMode.equals("Defined streets")) {
				editOverlay=definedRoadsOverlay;
				 geometryEditor.stop();
				esistingStreets.setSelected(true);
				newStreets.setSelected(false);
			} else if (selectionMode.equals("New streets")) {
				editOverlay=newOverlay;
				 geometryEditor.stop();
				esistingStreets.setSelected(false);
				newStreets.setSelected(true);
			}

		});
		streetBox.setSpacing(10);
		streetBox.getChildren().addAll(streetLabel,newStreets, esistingStreets);
		
		VBox mapToolBox = new VBox();
		mapToolBox.setSpacing(10);
		mapToolBox.setPrefHeight(30);
		mapToolBox.setPrefWidth(100);
		mapToolBox.setMaxSize(300, 50);
		mapToolBox.setPadding(new Insets(10, 10, 10, 10));
		mapToolBox.setBackground(new Background(background_fill));

		// SAVE and CANCEL buttons
		HBox editButtonBox = new HBox();
		editButtonBox.setSpacing(10);
		Button delete = new Button("Delete selected road");
		delete.setOnAction(this::deleteSelectedGraphic);
		Button edit = new Button("Edit selected road");
		edit.setOnAction(this::editSelectedGraphic);
		editButtonBox.getChildren().addAll(delete,edit );
		
		// SAVE and CANCEL buttons
		HBox exitButtonBox = new HBox();
		exitButtonBox.setSpacing(10);
		Button cancel = new Button("Cancel");
		cancel.setOnAction(this::cancel);
		Button save = new Button("Save & exit");
		save.setOnAction(this::saveAllRoads);
		exitButtonBox.getChildren().addAll(cancel,save );

		Separator separator1 = new Separator();
//		toggleBox

		mapToolBox.getChildren().addAll(viewBox,streetBox,editButtonBox,exitButtonBox);
		StackPane.setAlignment(mapToolBox, Pos.TOP_LEFT);
		StackPane.setMargin(mapToolBox, new Insets(10, 0, 0, 10));
		rootControl.getChildren().add(mapToolBox);

	}


	/**
	 * GEts a final feature layer from the operational layers of the current
	 * mapView. This helper class is designed to get the layer as a final variable
	 * to be used in listener callbacks
	 * 
	 * @param featureLayerId - layer unique Identifier
	 * @return final FeatureLayer
	 */
	private final FeatureLayer getFeatureFromMap(String featureLayerId) {
		// get the layer based on its Id
		FeatureCollectionLayer featureLayers = getFeatureCollectionLayersFromMap();
		for (FeatureLayer layer : featureLayers.getLayers()) {

			if (layer.getName().equals(featureLayerId)) {
				return (FeatureLayer) layer;

			}
		}
		return null;
	}

	/**
	 * Sets the Styles for all operational layers. To be called AFTER all
	 * operational layers are added!
	 * 
	 * @param isBrownfield - whether this application should be simple - simple
	 *                     means no roads and builings
	 * @param isMorphology
	 */
	public void setStyles(boolean isBrownfield, boolean isMorphology) {
		FeatureCollectionLayer featureLayers = getFeatureCollectionLayersFromMap();

		FeatureLayer nodePolygonLayer = null;
		FeatureLayer nodePointLayer = null;
		FeatureLayer facilityLayer = null;
		FeatureLayer roadLayer = null;
		FeatureLayer buildingLayer = null;
		FeatureLayer feederMiles = null;
		for (FeatureLayer layer : featureLayers.getLayers()) {
			if (layer.getName().equals(LayerID.NODE_POINT.toString())) {
				nodePointLayer = layer;
			} else if (layer.getName().equals(LayerID.NODE_POLYGON.toString())) {
				nodePolygonLayer = layer;
			} else if (layer.getName().equals(LayerID.FACILITY.toString())) {
				facilityLayer = layer;
			} else if (layer.getName().equals(LayerID.ROADS.toString())) {
				roadLayer = layer;
			} else if (layer.getName().equals(LayerID.BUILDINGS.toString())) {
				buildingLayer = layer;
			} else if (layer.getName().equals(LayerID.FEEDER_MILES.toString())) {
				feederMiles = layer;
			}

			// Style facility, node point, and node polygon
			Renderer r = this.mapStyle.getRenderer(layer, isBrownfield, isMorphology);
			layer.setRenderer(r);
			layer.setVisible(false);

		}

		if (feederMiles != null) {
			feederMiles.setVisible(true);
		}

		// set visible here
		if (nodePolygonLayer != null) { // 0
			nodePolygonLayer.setVisible(true);
		}

		if (roadLayer != null && !isBrownfield && !isMorphology) { // 0 or 1
			roadLayer.setVisible(true);
		}
		if (facilityLayer != null) { // 1 or 2
			// census calculation display facility
			if (isBrownfield) {
				facilityLayer.setVisible(false);
			} else {
				facilityLayer.setVisible(true);
			}
		}
		if (nodePointLayer != null) { // 2 or 3
			nodePointLayer.setVisible(true);
		}

		if (buildingLayer != null && !isBrownfield && !isMorphology) { // last, doesnt really matter
			buildingLayer.setVisible(true);
		}

		this.mapView.setOpaqueInsets(new Insets(20, 20, 20, 20));

	}

	/**
	 * returns the first featureCollectionLayer found. Should only be one for our
	 * json files
	 * 
	 * @return
	 */
	private final FeatureCollectionLayer getFeatureCollectionLayersFromMap() {
		for (Layer layer : mapView.getMap().getOperationalLayers()) {
			if (layer instanceof FeatureCollectionLayer) {
				return (FeatureCollectionLayer) layer;
			}
		}
		return null;
	}

	/**
	 * Disposes the mapview
	 */
	public void dispose() {
		if (this.mapView != null) {
			this.mapView.dispose();

		}

	}
	private void setExpansionVisible() {

		GraphicsOverlay feeder = graphicOverlayMap.get(LayerID.FEEDER_MILES.toString());
		if (feeder != null) {
			feeder.setVisible(false);
		}
		GraphicsOverlay nodePoint = graphicOverlayMap.get(LayerID.NODE_POINT.toString());
		if (nodePoint != null) {
			nodePoint.setVisible(true);
		}
		GraphicsOverlay fac = graphicOverlayMap.get(LayerID.FACILITY.toString());
		if (fac != null) {
			fac.setVisible(true);
		}
		GraphicsOverlay distro = graphicOverlayMap.get(LayerID.ROADS.toString());
		if (distro != null) {
			distro.setVisible(true);
		}
		GraphicsOverlay buildings = graphicOverlayMap.get(LayerID.BUILDINGS.toString());
		if (buildings != null) {
			buildings.setVisible(true);
		}
	}

	/**
	 * Blocks main BuildMap2 thread until background thread loads map
	 */
	public void blockUntilLoaded() {
		while (!loaded) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	private String formatRoadAttr(Map<String, Object> roadAttr) {
		String formattedString = "";
		/** Node specific metadata */
		// [1] Nodes C=onnected
		for (String attr: roadAttr.keySet()) {
			Object o =roadAttr.get(attr);
			formattedString += String.format("%s  %s\n", attr + ":", o.toString());
		}
		return formattedString;
	}

	private class GraphAndLayer {
		public Graphic graph;
		public GraphicsOverlay layer;
		/**
		 * @param graph
		 * @param layer
		 */
		public GraphAndLayer(Graphic graph, GraphicsOverlay layer) {
			this.graph = graph;
			this.layer = layer;
		}
		
		
		
	}
}
