package com;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.Scanner;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Pagination;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import javafx.util.StringConverter;

/**
 * <h1>Controller Class for JavaFX Project</h1>
 * Class used for interfacing between the JavaFX GUI and the 
 * back-end calculations. Binds all the actions of the GUI and 
 * creates nodes such as the PDF preview pagination.
 * <p>
 * Credit to <a href = "https://github.com/james-d/PdfViewer">james-d</a>
 * for sections of the code relating to the PDF previewing.
 * @author Jagan Prem, james-d
 */
public class Controller {

	@FXML private Stage stage;
	@FXML private TabPane tabPane;
	@FXML private Pagination pagination;
	@FXML private Label currentZoomLabel;
	@FXML private BorderPane previewPane;
	@FXML private ScrollPane scroller;
	@FXML private Spinner<Integer> spinner;
	@FXML private Button saveButton;
	@FXML private Button emailButton;
	@FXML private ListView<String> results;
	@FXML private ListView<String> restrictionList;
	@FXML private CheckBox checkbox9;
	@FXML private CheckBox checkbox10;
	@FXML private CheckBox checkbox11;
	@FXML private CheckBox checkbox12;
	@FXML private Label merWarning;
	@FXML private Label periodWarning;
	@FXML private TextField textField;
	@FXML private Button merButton;
	@FXML private ColorPicker bgColorPicker;
	@FXML private ColorPicker fgColorPicker;
	@FXML private ColorPicker textColorPicker;
	@FXML private CheckBox checkboxM;
	@FXML private CheckBox checkboxF;

	private ObjectProperty<PDFFile> currentFile;
	private ObjectProperty<ImageView> currentImage;
	private DoubleProperty zoom;
	private PageDimensions currentPageDimensions;
	private PDDocument doc;
	private File file;
	private ExecutorService imageLoadService;
	
	private int period;
	private boolean fitOnLoad;
	private String pathToMer;
	
	private ClassPeriodDistanceComparator distanceComparator;
	private HashMap<String, Integer> roomDistances;
	private SeatingHandler seatingHandler;
	private ClassPeriodFiller classPeriodFiller;
	private ArrayList<ArrayList<ClassPeriod>> periods;
	private ArrayList<Integer> grades;
	private ArrayList<ClassPeriod> classes;
	private HashMap<String, String> emailAddresses;
	private ArrayList<ClassPeriod> classCopy;
	private AutoCompleteSearchHandler searchBox;
	private String backgroundColor;
	private String foregroundColor;
	private String textColor;
	private String email;
	private String password;
	private boolean male;
	private boolean female;
	
	private static final double ZOOM_DELTA = 1.2;
	
	/**
	 * Initializes the properties of the GUI and creates the objects
	 * involved in the back-end calculations for the seating chart
	 * generation.
	 * @see AutoCompleteSearchHandler
	 * @see ClassPeriodDistanceComparator
	 * @see ClassPeriodFiller
	 * @see SeatingHandler
	 */
	public void initialize() {
		male = true;
		female = true;
		backgroundColor = "#f4f4f4";
		foregroundColor = "#f4f4f4";
		textColor = "#292929";
		
		try {
			readConfigFile();
			classPeriodFiller = new ClassPeriodFiller(pathToMer);
			merButton.setText(new File(pathToMer).getName());
			periods = classPeriodFiller.fillPeriods();
		}
		catch (Exception e) {
			periods = null;
			merWarning.setText(".MER file not selected, not found, or invalid. Go to settings to change the .MER file.");
			if (!(e instanceof FileNotFoundException))
				showErrorMessage("Invalid .MER file", e);
		}
				
		bindColorPickers();
		bgColorPicker.setValue(Color.web(backgroundColor));
		fgColorPicker.setValue(Color.web(foregroundColor));
		textColorPicker.setValue(Color.web(textColor));
		updateColors();
		
		roomDistances = new HashMap<String, Integer>();
		setRoomDistances();
		
		distanceComparator = new ClassPeriodDistanceComparator(roomDistances);
		grades = new ArrayList<Integer>();
		
		int[][][] rowSizes =
			{
				{
					{7, 8, 8, 9, 10, 8, 11, 12, 12, 13, 14, 14, 14, 14},
					{9, 10, 10, 10, 11, 9, 12, 12, 12, 13, 14, 14, 14, 14},
					{7, 8, 8, 9, 10, 10, 11, 12, 12, 13, 14, 8, 8, 8}
				},
				{
					{9, 8, 13, 13, 12, 11, 11, 11},
					{15, 15, 15, 15, 10, 10, 5}
				}
			};
		
		seatingHandler = new SeatingHandler(rowSizes);
		
		createAndConfigureImageLoadService();
		currentFile = new SimpleObjectProperty<>();
		currentImage = new SimpleObjectProperty<>();
		scroller.contentProperty().bind(currentImage);
		
		zoom = new SimpleDoubleProperty(1);
		bindZoomKeys();
		
		bindPaginationToCurrentFile();
		createPaginationPageFactory();
		restrictSpinner();
		bindCheckBoxes();
		
		File imageFile = new File("save.png");
		Image image = new Image(imageFile.toURI().toString());
		saveButton.setGraphic(new ImageView(image));
		bindSaveButton();
		
		email = "370149@mcpsmd.net";
		//YOOO DON'T LOOK AT THE NEXT LINE PLS
		password =                                                                                                                                                                                                                                                                              "Ydis7948";
		emailAddresses = new HashMap<String, String>();
		readEmailAddresses();
		imageFile = new File("email.png");
		image = new Image(imageFile.toURI().toString());
		emailButton.setGraphic(new ImageView(image));
		bindEmailButton();
		
		searchBox = new AutoCompleteSearchHandler(textField, results, restrictionList);
		
		try {
			prepareTempFile();
		} catch (IOException e) {
			showErrorMessage("Could not load template", e);
			e.printStackTrace();
		}
		loadFile(file);
	}
	
	/**
	 * Reads in the values from .config (i.e. the location of the
	 * .MER file and the the GUI colors.)
	 * @throws FileNotFoundException When .config is not found.
	 */
	private void readConfigFile() throws FileNotFoundException {
		Scanner scanner = new Scanner(new File(".config"));
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			String value = line.substring(line.indexOf(":") + 1);
			if (line.startsWith("MER"))
				pathToMer = value;
			else if (line.startsWith("BAC"))
				backgroundColor = value;
			else if (line.startsWith("FOR"))
				foregroundColor = value;
			else if (line.startsWith("TEX"))
				textColor = value;
		}
		scanner.close();
	}
	
	/**
	 * Converts a Color object to a String of a hexadecimal color 
	 * format, prefixed by a number symbol.
	 * @param color The Color object to be converted.
	 * @return String The hexadecimal string.
	 * @see Color
	 */
	public static String toRGBCode(Color color)
    {
        return String.format( "#%02X%02X%02X",
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255));
    }
	
	/**
	 * Connects the ColorPicker node of the GUI to the change of
	 * the colors of the other node.
	 * @see ColorPicker
	 * @see #bgColorPicker
	 * @see #fgColorPicker
	 * @see #textColorPicker
	 */
	private void bindColorPickers() {
		EventHandler<ActionEvent> eventHandler = new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				backgroundColor = toRGBCode(bgColorPicker.getValue());
				foregroundColor = toRGBCode(fgColorPicker.getValue());
				textColor = toRGBCode(textColorPicker.getValue());
				updateColors();
				try {
					updateConfigFile();
				}
				catch (FileNotFoundException e) {}
			}
		};
		bgColorPicker.setOnAction(eventHandler);
		fgColorPicker.setOnAction(eventHandler);
		textColorPicker.setOnAction(eventHandler);
	}
	
	/**
	 * Changes the colors of the GUI elements to match the selected 
	 * colors.
	 */
	private void updateColors() {
		tabPane.setStyle("-fx-font: 24 calibri; -fx-background: " + backgroundColor + "; -fx-color: " + foregroundColor + "; -fx-mid-text-color: "
						+ textColor + "; -fx-dark-text-color: " + textColor + ";" + "; -fx-light-text-color: " + textColor + ";");
	}
	
	/**
	 * Writes to .config with the updated settings.
	 * @throws FileNotFoundException When .config is not found.
	 */
	private void updateConfigFile() throws FileNotFoundException {
		PrintWriter out = new PrintWriter(".config");
		if (pathToMer != null)
			out.println("MER:" + pathToMer);
		out.println("BAC:" + backgroundColor);
		out.println("FOR:" + foregroundColor);
		out.println("TEX:" + textColor);
		out.close();
	}
	
	/**
	 * Assigns a distance constant to each of the room numbers of
	 * the school.
	 * @see #roomDistances
	 */
	private void setRoomDistances() {
		String[] roomsInOrder = new String[] {
			"64",
			"63",
			"61",
			"60",
			"54",
			"9",
			"55",
			"57",
			"58",
			"6",
			"13",
			"12",
			"11",
			"10",
			"42",
			"51",
			"44",
			"50",
			"45",
			"46",
			"4",
			"3",
			"14",
			"GYM1",
			"GYM2",
			"48",
			"2",
			"49",
			"15",
			"47",
			"2",
			"18",
			"21",
			"22",
			"41",
			"19",
			"20",
			"36",
			"38",
			"39",
			"17",
			"16",
			"34",
			"35",
			"1",
			"24",
			"23"
		};
		for (int i = 0; i < roomsInOrder.length; i++)
			roomDistances.put(roomsInOrder[i], i);
	}
	
	/**
	 * Binds the six checkboxes to change which grades are active
	 * and which genders.
	 * @see #checkbox9
	 * @see #checkbox10
	 * @see #checkbox11
	 * @see #checkbox12
	 * @see #checkboxM
	 * @see #checkboxF
	 * @see #grades
	 * @see #male
	 * @see #female
	 */
	private void bindCheckBoxes() {
		Function<Integer, ChangeListener<Boolean>> createListener = (grade) -> {
			return new ChangeListener<Boolean>() {
				@Override
			    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
			        if (newValue && !oldValue)
			        	grades.add(grade);
			        else if (!newValue && oldValue)
			        	grades.remove(grade);
			        updateClasses();
				}
			};
		};
		checkbox9.selectedProperty().addListener(createListener.apply(9));
		checkbox10.selectedProperty().addListener(createListener.apply(10));
		checkbox11.selectedProperty().addListener(createListener.apply(11));
		checkbox12.selectedProperty().addListener(createListener.apply(12));
		checkboxM.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
		    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
		        if (newValue && !oldValue)
		        	male = true;
		        else if (!newValue && oldValue)
		        	male = false;
		        updateClasses();
			}
		});
		checkboxF.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
		    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
		        if (newValue && !oldValue)
		        	female = true;
		        else if (!newValue && oldValue)
		        	female = false;
		        updateClasses();
			}
		});
	}
	
	/**
	 * Updates the list of classes to reflect the settings chosen
	 * on the GUI.
	 * @see #classes
	 * @see ClassPeriod
	 * @see #period
	 * @see #periods
	 */
	private void updateClasses() {
		classes = new ArrayList<ClassPeriod>();
		if (period != 0) {
			if (periods != null) {
				ArrayList<ClassPeriod> classPeriods = periods.get(period - 1);
				for (ClassPeriod classPeriod : classPeriods)
					for (int grade : grades)
						if (classPeriod.getClassSize(grade, male, female) > 0) {
							classes.add(classPeriod);
							break;
						}
			}
			periodWarning.setText("");
		}
		else
			periodWarning.setText("\nNo period selected.");
		searchBox.setChoices(classes);
	}
	
	/**
	 * Restricts the spinner for period selection so that the text
	 * entered meets specific requirements, such as that no non-
	 * numeric characters are typed in.
	 * @see #spinner
	 * @see #period
	 */
	private void restrictSpinner() {
		period = 1;
		IntegerSpinnerValueFactory valueFactory = new IntegerSpinnerValueFactory(1, 8);
        UnaryOperator<TextFormatter.Change> filter = change -> {
			String newText = change.getControlNewText();
			String changeText = change.getText();
		    if (changeText.matches("[1-8]?")) {
		    	if (newText.length() < 2) {
			    	if (change.isContentChange()) {
			    		period = newText.equals("") ? 0 : Integer.parseInt(newText);
			    		updateClasses();
			    	}
			    	return change;
		    	}
		    	else
		    		valueFactory.setValue(Integer.parseInt(newText.substring(0, 1)));		    		
		    }
		    else if (changeText.equals("-"))
		    	zoomOut();
		    else if (changeText.equals("="))
		    	zoomIn();
		    return null;
		};
		TextFormatter<Integer> textFormatter = new TextFormatter<Integer>(filter);
		spinner.getEditor().setTextFormatter(textFormatter);
        valueFactory.setConverter(new StringConverter<Integer>() {
            @Override
            public String toString(Integer object) {
                return object.toString();
            }

            @Override
            public Integer fromString(String string) {
                if (string.matches("-?\\d+"))
                    return new Integer(string);
                return 0;
            }
        });
        spinner.setValueFactory(valueFactory);
        spinner.getEditor().addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
        	@Override
			public void handle(KeyEvent event) {
				if (event.getCode() == KeyCode.UP)
					valueFactory.increment(1);
				if (event.getCode() == KeyCode.DOWN)
					valueFactory.decrement(1);
			}
		});
	}
	
	/**
	 * Creates a temporary file in the temp folder and loads the
	 * current PDF document from the template file.
	 * @throws IOException
	 * @see #file
	 * @see #doc
	 */
	private void prepareTempFile() throws IOException {
		doc = PDDocument.load(new File("template.pdf"));
		for (File toDel : new File("temp").listFiles())
			if (toDel.getName().endsWith(".pdf"))
				toDel.delete();
		file = File.createTempFile("seating-chart", ".pdf", new File("temp"));
		doc.save(file);
		file.deleteOnExit();
	}
	
	/**
	 * Binds the save button to open a save dialog when clicked.
	 * @see #saveButton
	 */
	private void bindSaveButton() {
		saveButton.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {
				try {
					FileChooser fileChooser = new FileChooser();
					fileChooser.setTitle("Save Seating Chart");
					ExtensionFilter extensionFilter = new ExtensionFilter("PDF File", Arrays.asList(new String[] {"*.pdf"}));
					fileChooser.getExtensionFilters().add(extensionFilter);
					File file = fileChooser.showSaveDialog(stage);
					doc.save(file);
				}
				catch (IOException e) {
					showErrorMessage("Could not save file", e);
					e.printStackTrace();
				}
				catch (NullPointerException e) {}
			}
		});
	}
	
	/**
	 * Reads the email addresses in from emails.csv.
	 * @see #emailAddresses
	 */
	private void readEmailAddresses() {
		try {
			Scanner scanner = new Scanner(new File("emails.csv"));
			while (scanner.hasNextLine()) {
				String[] line = scanner.nextLine().split(":");
				emailAddresses.put(line[0], line[1]);
			}
			scanner.close();
		}
		catch (FileNotFoundException e) {}
	}
	
	/**
	 * Converts the list of current classes to a list of the
	 * associated teacher email addresses.
	 * @return ArrayList&lt;String&gt; The list of email addresses.
	 * @see #classCopy
	 * @see #emailAddresses
	 */
	private ArrayList<String> getRecipients() {
		ArrayList<String> recipients = new ArrayList<String>();
		for (ClassPeriod classPeriod : classCopy) {
			String email = emailAddresses.get(classPeriod.toString().replaceAll(" ", "").toLowerCase());
			if (email != null)
				recipients.add(email);
		}
		return recipients;
	}
	
	/**
	 * Sends an email to the specified recipient addresses with a
	 * provided subject and body text and with the seating chart
	 * attached.
	 * @param recipients The email addresses to send the chart to.
	 * @param subject The subject line of the emails sent out.
	 * @param body The message body of the emails to be sent.
	 */
	private void sendEmail(ArrayList<String> recipients, String subject, String body) {
		Properties props = new Properties();
		props.put("mail.transport.protocol", "smtp");
		props.put("mail.smtp.host","smtp.gmail.com");
		props.put("mail.smtp.port", "587");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable","true");
		Session session = Session.getDefaultInstance(props, new Authenticator() {
			@Override
	        protected PasswordAuthentication getPasswordAuthentication() {
	            return new PasswordAuthentication(email, password);
	        }
		});
		Message message = new MimeMessage(session);
		InternetAddress[] addresses = new InternetAddress[recipients.size()];
		for (int i = 0; i < addresses.length; i++) {
			try {
				addresses[i] = new InternetAddress(recipients.get(i));
			}
			catch (AddressException e) {}
		}
		try {
			message.setRecipients(RecipientType.BCC, addresses);
			message.setSubject(subject);
			Multipart multipart = new MimeMultipart();
			BodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setText(body);
			multipart.addBodyPart(messageBodyPart);
			messageBodyPart = new MimeBodyPart();
			DataSource source = new FileDataSource(file.getAbsolutePath());
			messageBodyPart.setDataHandler(new DataHandler(source));
			messageBodyPart.setFileName(file.getAbsolutePath());
			multipart.addBodyPart(messageBodyPart);
			message.setContent(multipart);
			Transport.send(message);
		}
		catch (MessagingException e) {e.printStackTrace();}
	}
	
	/**
	 * Binds the email button to open a dialog in which the user can
	 * enter a subject and body for the emails. When the button at
	 * the bottom is pressed, the email is sent the teachers currently
	 * on the seating chart.
	 * @see #emailButton
	 * @see #classCopy
	 */
	private void bindEmailButton() {
		emailButton.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {
				if (classCopy != null) {
					final Stage emailStage = new Stage();
					emailStage.initModality(Modality.APPLICATION_MODAL);
					emailStage.initOwner(stage);
					VBox emailVBox = new VBox();
					emailVBox.setStyle("-fx-font-size: 24;");
					
					TextField subject = new TextField();
					subject.setPromptText("Subject");
					subject.setAlignment(Pos.TOP_LEFT);
					subject.setStyle("-fx-focus-color: transparent;");
					
					TextField body = new TextField();
					body.setPromptText("Body");
					body.setMaxHeight(Integer.MAX_VALUE);
					body.setAlignment(Pos.TOP_LEFT);
					VBox.setVgrow(body, Priority.ALWAYS);
					body.setStyle("-fx-focus-color: transparent;");
					
					Button sendButton = new Button("Send Email");
					sendButton.setMaxWidth(Integer.MAX_VALUE);
					HBox.setHgrow(sendButton, Priority.ALWAYS);
					sendButton.setOnAction(new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent event) {
							String subjectLine = subject.getText(), bodyContent = body.getText();
							emailStage.close();
							try {
								ArrayList<String> recipients = getRecipients();
								sendEmail(recipients, subjectLine, bodyContent);
							}
							catch (Exception e) {
								if (e instanceof NullPointerException)
									showErrorMessage("No seating chart generated", e);
								else
									showErrorMessage("No email addresses/recipients", e);
							}
						}
					});
					
					emailVBox.getChildren().addAll(subject, body, sendButton);
					Scene emailScene = new Scene(emailVBox, 800, 600);
					emailStage.setScene(emailScene);
					emailVBox.requestFocus();
					emailStage.setTitle("Email Seating Chart to Teachers");
					File iconFile = new File("icon.png");
					Image icon = new Image(iconFile.toURI().toString());
					emailStage.getIcons().add(icon);
					emailStage.show();
				}
				else {
					showErrorMessage("No seating chart generated", new NullPointerException());
				}
			}
		});
	}
	
	/**
	 * Creates the method for the button used for selecting the .MER
	 * file, which creates an open file dialog for the user to select
	 * to select the file.
	 * @see #pathToMer
	 * @see #classPeriodFiller
	 * @see #periods
	 */
	@FXML public void selectMerFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Select .MER File");
		File mer = fileChooser.showOpenDialog(stage);
		if (mer != null) {
			pathToMer = mer.getAbsolutePath();
			try {
				classPeriodFiller = new ClassPeriodFiller(pathToMer);
				periods = classPeriodFiller.fillPeriods();
				updateClasses();
				merWarning.setText("");
			}
			catch (FileNotFoundException e) {
				periods = null;
				merWarning.setText(".MER file not selected, not found, or invalid. Go to settings to change the .MER file.");
				showErrorMessage("Invalid .MER file", e);
			}
			catch (Exception e) {
				periods = null;
				merWarning.setText(".MER file not selected, not found, or invalid. Go to settings to change the .MER file.");
			}
			merButton.setText(" " + mer.getName() + " ");
			try {
				updateConfigFile();
			} catch (FileNotFoundException e) {
				System.out.println("lol");
				System.out.println("what actually happened here tho");
			}
		}
	}
	
	/**
	 * Adds the values from the generated seating chart to the PDF
	 * template and flattens the form.
	 * @param doc The document to add the fields in.
	 * @param seatingChart The seating chart to fill in from.
	 * @throws IOException When a PDF field is not found.
	 */
	private void addAll(PDDocument doc, LinkedHashMap<String, Integer>[][][] seatingChart) throws IOException {
		PDDocumentCatalog docCatalog = doc.getDocumentCatalog();
		PDAcroForm acroForm = docCatalog.getAcroForm();
		int index = 0;
		for (int i = 0; i < seatingChart.length; i++) {
			for (int j = 0; j < seatingChart[i].length; j++) {
				for (int k = 0; k < seatingChart[i][j].length; k++)
					if (seatingChart[i][j][k].size() > 0) {
						PDField field = acroForm.getField(index + "_" + k);
						String value = " - ";
						boolean first = true;
						for (String teacher : seatingChart[i][j][k].keySet()) {
							if (!first) 
								value += ", ";
							else
								first = false;
							value += teacher + " " + seatingChart[i][j][k].get(teacher);
						}
						COSDictionary dict = field.getCOSObject();
						double size = 10;
						if (value.length() > 30) {
							size -= (value.length() - 30) / 4.3;
						}
				        dict.setString(COSName.DA, "/Helv " + size + " Tf 0 g");
				        field.getCOSObject().addAll(dict);
				        field.setValue(value);
					}
				index++;
			}
		}
		acroForm.flatten();
	}
	
	/**
	 * Creates a seating chart based on the specified conditions and
	 * saves it to a new temporary file. Handles any errors encountered
	 * in the process and displays fitting messages.
	 * @see #classes
	 * @see #classCopy
	 * @see #seatingHandler
	 * @see #grades
	 * @see #male
	 * @see #female
	 */
	@FXML public void generateSeatingChart() {
		seatingHandler.clear();
		if (pathToMer == null) {
			showErrorMessage("No .MER file given", new NullPointerException());
			return;
		}
		if (!pathToMer.endsWith(".mer")) {
			showErrorMessage("Invalid .MER file", new UnsupportedOperationException());
			return;
		}
		if (periods == null) {
			showErrorMessage(".MER file not found", new FileNotFoundException());
			return;
		}
		if (classes == null || (period == 0)) {
			showErrorMessage("No period given", new NullPointerException());
			return;
		}
		int size = 0;
		for (int grade : grades)
			for (ClassPeriod classPeriod : classes)
				size += classPeriod.getClassSize(grade, male, female);
		if (size > seatingHandler.capacity()) {
			showErrorMessage("Too many students", new IndexOutOfBoundsException());
			return;
		}
		classCopy = new ArrayList<ClassPeriod>(classes);
		ArrayList<ClassPeriod> restrictions = searchBox.getRestrictions();
		seatingHandler.fill(restrictions, grades, male, female);
		ArrayList<ClassPeriod> copy = new ArrayList<ClassPeriod>();
		for (ClassPeriod classPeriod : classes)
			if (!restrictions.contains(classPeriod))
				copy.add(classPeriod);
		seatingHandler.fill(copy, grades, male, female, distanceComparator);
		LinkedHashMap<String, Integer>[][][] seatingChart = seatingHandler.getSeatingChart();
		try {
			doc.close();
			prepareTempFile();
			addAll(doc, seatingChart);
			doc.save(file);
		}
		catch (IOException e) {
			showErrorMessage("Could not load/save temporary file", e);
			e.printStackTrace();
		}
		loadFile(file);		
	}
	
	/**
	 * Creates the image loader for the PDF preview and threads it.
	 * @see #imageLoadService
	 */
	private void createAndConfigureImageLoadService() {
		imageLoadService = Executors.newSingleThreadExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r);
				thread.setDaemon(true);
				return thread;
			}
		});
	}
	
	/**
	 * Binds the pagination to mirror the file that is currently loaded.
	 * @see #currentFile
	 * @see #pagination
	 * @author james-d
	 */
	private void bindPaginationToCurrentFile() {
		currentFile.addListener(new ChangeListener<PDFFile>() {
			@Override
			public void changed(ObservableValue<? extends PDFFile> observable, PDFFile oldFile, PDFFile newFile) {
				if (newFile != null) {
					pagination.setCurrentPageIndex(0);
				} 
			}
		});
		pagination.pageCountProperty().bind(new IntegerBinding() {
			{ super.bind(currentFile); }
			@Override
			protected int computeValue() {
				return currentFile.get() == null ? 0 : currentFile.get().getNumPages();
			}
		});
		pagination.disableProperty().bind(Bindings.isNull(currentFile));
	}
	
	/**
	 * Binds the zoom value to change the zoom of the pagination and adds
	 * listening for plus and minus to change the zoom of the preview.
	 * @see #zoom
	 * @see #currentZoomLabel
	 * @see #pagination
	 * @author james-d
	 */
	private void bindZoomKeys() {
		zoom.addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				updateImage(pagination.getCurrentPageIndex());
			}
		});
		currentZoomLabel.textProperty().bind(Bindings.format("%.0f %%", zoom.multiply(100)));
		tabPane.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			public void handle(KeyEvent event) {
				if (event.getCode() == KeyCode.EQUALS)
					zoomIn();
				if (event.getCode() == KeyCode.MINUS)
					zoomOut();
			}
		});
	}
	
	/**
	 * Binds the pagination to change with the page and file selection.
	 * @see #pagination
	 * @see #currentFile
	 * @author james-d
	 */
	private void createPaginationPageFactory() {
		pagination.setPageFactory(new Callback<Integer, Node>() {
			@Override
			public Node call(Integer pageNumber) {
				if (currentFile.get() == null)
					return null;
				else {
					if (pageNumber >= currentFile.get().getNumPages() || pageNumber < 0) {
						return null;
					} else {
						updateImage(pageNumber);
						return scroller;
					}
				}
			}
		});
	}
	
	/**
	 * Loads the specified PDF file into the preview pagination.
	 * @param file The File object to be loaded.
	 * @see #pagination
	 * @see #fitOnLoad
	 * @author james-d
	 */
	private void loadFile(File file) {
		if (file != null) {
			final Task<PDFFile> loadFileTask = new Task<PDFFile>() {
				@Override
				protected PDFFile call() throws Exception {
					try ( 
							RandomAccessFile raf = new RandomAccessFile(file, "r");
							FileChannel channel = raf.getChannel() 
						) {
						ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
						return new PDFFile(buffer);
					}
				}
			};
			loadFileTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
				@Override
				public void handle(WorkerStateEvent event) {
					pagination.getScene().getRoot().setDisable(false);
					final PDFFile pdfFile = loadFileTask.getValue();
					currentFile.set(pdfFile);
					updateImage(pagination.getCurrentPageIndex());
				}
			});
			loadFileTask.setOnFailed(new EventHandler<WorkerStateEvent>() {
				@Override
				public void handle(WorkerStateEvent event) {
					pagination.getScene().getRoot().setDisable(false);
					showErrorMessage("Could not load file "+file.getName(), loadFileTask.getException());
				}
			});
			fitOnLoad = true;
			imageLoadService.submit(loadFileTask);
		}
	}
	
	/**
	 * Method for the zoom in button to increase the zoom of the preview.
	 * Zooms by a factor of {@value #ZOOM_DELTA}.
	 * @see #zoomOut()
	 * @see #zoomFit()
	 * @see #ZOOM_DELTA
	 * @author james-d
	 */
	@FXML private void zoomIn() {
		zoom.set(zoom.get() * ZOOM_DELTA);
	}
	
	/**
	 * Method for the zoom out button to decrease the zoom of the preview.
	 * Zooms by a factor of {@value #ZOOM_DELTA}.
	 * @see #zoomIn()
	 * @see #zoomFit()
	 * @see #ZOOM_DELTA
	 * @author james-d
	 */
	@FXML private void zoomOut() {
		zoom.set(zoom.get() / ZOOM_DELTA);
	}
	
	/**
	 * Method for the zoom fit button to match the size of the preview to
	 * the size of the pagination.
	 * @see #zoomIn()
	 * @see #zoomOut()
	 * @author james-d
	 */
	@FXML private void zoomFit() {
		double horizontalZoom = (scroller.getWidth() - 20) / currentPageDimensions.width;
		double verticalZoom = (scroller.getHeight() - 20) / currentPageDimensions.height;
		zoom.set(Math.min(horizontalZoom, verticalZoom));
	}
	
	/**
	 * Updates the pagination preview with an image node rendered from the
	 * selected PDF file at the current zoom level; zooms to fit if directly
	 * after loading the file.
	 * @param pageNumber The page number of the PDF file to render.
	 * @see #pagination
	 * @see #fitOnLoad
	 * @author james-d
	 */
	private void updateImage(final int pageNumber) {
		final Task<ImageView> updateImageTask = new Task<ImageView>() {
			@Override
			protected ImageView call() throws Exception {
				PDFPage page = currentFile.get().getPage(pageNumber + 1);
				Rectangle2D bbox = page.getBBox();
				final double actualPageWidth = bbox.getWidth();
				final double actualPageHeight = bbox.getHeight();
				currentPageDimensions = new PageDimensions(actualPageWidth, actualPageHeight);
				final int width = (int) (actualPageWidth * zoom.get());
				final int height = (int) (actualPageHeight * zoom.get());
				java.awt.Image awtImage = page.getImage(width, height, bbox, null, true, true); 
				BufferedImage buffImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
				buffImage.createGraphics().drawImage(awtImage, 0, 0, null);
				Image image = SwingFXUtils.toFXImage(buffImage, null);
				ImageView imageView = new ImageView(image);
				imageView.setPreserveRatio(true);
				return imageView;
			}
		};

		updateImageTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent event) {
				pagination.getScene().getRoot().setDisable(false);
				currentImage.set(updateImageTask.getValue());
				if (fitOnLoad) {
					zoomFit();
					fitOnLoad = false;
				}
			}
		});
		
		updateImageTask.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent event) {
				pagination.getScene().getRoot().setDisable(false);
				updateImageTask.getException().printStackTrace();
			}
			
		});
		
		pagination.getScene().getRoot().setDisable(true);
		imageLoadService.submit(updateImageTask);
	}
	
	/**
	 * Shows an error message on the GUI based on a provided message and
	 * exception.
	 * @param message The message to display on the modal box.
	 * @param exception The exception to show in detail.
	 * @author james-d
	 */
	private void showErrorMessage(String message, Throwable exception) {
		final Stage dialog = new Stage();
		dialog.initModality(Modality.APPLICATION_MODAL);
		dialog.initOwner(stage);
		dialog.initStyle(StageStyle.UNDECORATED);
		final VBox root = new VBox(10);
		root.setStyle("-fx-border-color: black;");
		root.setPadding(new Insets(10));
		StringWriter errorMessage = new StringWriter();
		exception.printStackTrace(new PrintWriter(errorMessage));
		final Label detailsLabel = new Label(errorMessage.toString());
		TitledPane details = new TitledPane();
		details.setText("Exception:");
		Label briefMessageLabel = new Label(message);
		briefMessageLabel.setStyle("-fx-font-size: 36");
		final HBox detailsLabelHolder =new HBox();
		
		Button closeButton = new Button("OK");
		closeButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				dialog.close();
			}
		});
		HBox closeButtonHolder = new HBox();
		closeButtonHolder.getChildren().add(closeButton);
		closeButtonHolder.setAlignment(Pos.CENTER);
		closeButtonHolder.setPadding(new Insets(5));
		root.getChildren().addAll(briefMessageLabel, details, detailsLabelHolder, closeButtonHolder);
		details.setExpanded(false);
		details.setAnimated(false);

		details.expandedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable,
					Boolean oldValue, Boolean newValue) {
				if (newValue)
					detailsLabelHolder.getChildren().add(detailsLabel);
				else
					detailsLabelHolder.getChildren().remove(detailsLabel);
				dialog.sizeToScene();
			}
		});
		final Scene scene = new Scene(root);
		
		dialog.setScene(scene);
		dialog.show();
	}
	
	/**
	 * Struct-like class intended to represent the physical dimensions of a page
	 * in pixels (as opposed to the dimensions of the (possibly zoomed) view.
	 * Used to compute zoom factors for zoomToFit and zoomToWidth.
	 * @author james-d
	 */
	private class PageDimensions {
		private double width;
		private double height;
		
		PageDimensions(double width, double height) {
			this.width = width;
			this.height = height;
		}
		
		@Override
		public String toString() {
			return String.format("[%.1f, %.1f]", width, height);
		}
	}

}