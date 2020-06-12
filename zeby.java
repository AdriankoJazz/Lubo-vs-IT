import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.apache.poi.ss.usermodel.*;

import com.itextpdf.text.*;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import java.util.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Main extends Application {

    public static Stage window;
    public static VBox vbox;
    public static Label message;

    public static File file, directory;

    public static void main(String[] args){
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        window = primaryStage;
        window.setTitle("Feedback Excel to PDF converter");

        vbox = new VBox();
        vbox.setPrefWidth(720);
        vbox.setPrefHeight(480);
        vbox.setSpacing(10);
        vbox.setAlignment(Pos.TOP_CENTER);

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("XLS", "*.xls"),
                new FileChooser.ExtensionFilter("XLSX", "*.xlsx")
        );
        DirectoryChooser directoryChooser = new DirectoryChooser();

        Label inputLabel = new Label("no input file selected");
        Label outputLabel = new Label("no output directory selected");
        message = new Label("");

        Button inputButton = new Button("select input file");
        inputButton.setOnAction(event -> {
            file = fileChooser.showOpenDialog(primaryStage);
            inputLabel.setText(file.getAbsolutePath());
        });

        Button outputButton = new Button("select output directory");
        outputButton.setOnAction(event -> {
            directory = directoryChooser.showDialog(primaryStage);
            outputLabel.setText(directory.getAbsolutePath());
        });

        Button startButton = new Button("create");
        startButton.setOnAction(event -> {
            if (file != null && directory != null)
                createHash(file, directory);
        });

        HBox inputLine = new HBox();
        inputLine.getChildren().addAll(inputButton, inputLabel);
        inputLine.setSpacing(10);
        inputLine.setAlignment(Pos.CENTER);
        inputLine.setPadding(new Insets(10, 0, 0, 0));

        HBox outputLine = new HBox();
        outputLine.getChildren().addAll(outputButton, outputLabel);
        outputLine.setSpacing(10);
        outputLine.setAlignment(Pos.CENTER);
        outputLine.setPadding(new Insets(0, 0, 20, 0));

        vbox.getChildren().addAll(inputLine, outputLine, startButton, message);

        window.setScene(new Scene(vbox));
        window.show();
    }

    private void createHash(File file, File directory) {
        Row rowCurrent;
        Cell cellMaster;
        Teacher teacherCurrent = null;
        HashMap<String, Teacher> teachers = new HashMap<>();

        try {
            FileInputStream inputStream = new FileInputStream(file);
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);    // pozor na cislo sheetu

            Iterator<Row> iteratorRow = sheet.iterator();

            iteratorRow.next();      //
            iteratorRow.next();      // aby sme zacali na riadku index 2
            while (iteratorRow.hasNext()) {
                rowCurrent = iteratorRow.next();

                Iterator<Cell> iteratorCell = sheet.getRow(0).cellIterator();   // tam su otazky
                iteratorCell.next();    // aby sme zacali na cell index 1
                while (iteratorCell.hasNext()) {
                    cellMaster = iteratorCell.next();

                    if (rowCurrent.getCell(cellMaster.getColumnIndex()) != null && cellMaster.getStringCellValue().contains("Pick your")) {
                        if (teachers.containsKey(rowCurrent.getCell(cellMaster.getColumnIndex()).getStringCellValue())) {
                            teacherCurrent = teachers.get(rowCurrent.getCell(cellMaster.getColumnIndex()).getStringCellValue());
                        } else if (!rowCurrent.getCell(cellMaster.getColumnIndex()).getStringCellValue().contains(" not ")) {
                            teacherCurrent = new Teacher(rowCurrent.getCell(cellMaster.getColumnIndex()).getStringCellValue());
                            teachers.put(teacherCurrent.getNameSubject(), teacherCurrent);
                        } else {
                            teacherCurrent = null;
                        }
                    } else if (teacherCurrent != null && sheet.getRow(1).getCell(cellMaster.getColumnIndex()) != null && // aby sme vedeli ze ci numericka otazka a jej maxScore
                            rowCurrent.getCell(cellMaster.getColumnIndex()) != null) {
                        if (sheet.getRow(1).getCell(cellMaster.getColumnIndex()).getNumericCellValue() == 0) {
                            if (cellMaster.getStringCellValue().contains("Characterize")) {   // characterize je na subject
                                teacherCurrent.addSubjectEval(rowCurrent.getCell(cellMaster.getColumnIndex()).getStringCellValue());
                            } else if (cellMaster.getStringCellValue().contains("What are")) {      // what are je na Teacher
                                teacherCurrent.addTeacherEval(rowCurrent.getCell(cellMaster.getColumnIndex()).getStringCellValue());
                            }
                        } else if (sheet.getRow(1).getCell(cellMaster.getColumnIndex()).getNumericCellValue() > 0) {
                            teacherCurrent.addScore(cellMaster.getStringCellValue(), rowCurrent.getCell(cellMaster.getColumnIndex()).getNumericCellValue(), 5);
                        }
                        teachers.replace(teacherCurrent.getNameSubject(), teacherCurrent);
                    }
                }
            }

            Vector<Teacher> teachersVector = new Vector<>(5, 5);
            Iterator hmIterator = teachers.entrySet().iterator();
            while (hmIterator.hasNext()) {
                Map.Entry mapElement = (Map.Entry) hmIterator.next();
                teachersVector.add((Teacher) mapElement.getValue());
            }

            message.setText("Done reading Excel file.");

            PdfGenerator pdfGenerator = new PdfGenerator();

            for (int i = 0; i < teachersVector.size(); i++) {

                pdfGenerator.createPdf(teachersVector.get(i), directory);

                /*
                TODO: Print message "ratings_[subject_name].pdf generated"
                TODO: Print message "subjecteval_[subject_name].pdf generated" after I write the function that generates
                TODO: subject evaluation pdfs
                TODO: Print message "teachereval_[subject_name].pdf generated" after I write the function that generates
                TODO: teacher evaluation pdfs
                */

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class PdfGenerator {

    PdfGenerator() {}

    public void createPdf(Teacher teacher, File directory) throws IOException, DocumentException {

        //Vezmem reku hodnotenia ucitela
        HashMap ratings = teacher.getHashMap();
        //Vytvorim si tuto vec aby som mohol iterovat cez hash mapu
        Iterator it = ratings.entrySet().iterator();

        //Na nazvy suborov
        int filenumber = 1;

        //Veci na path kde sa to ulozi a nazov suboru
        String path = directory.getPath()+"\\";
        String filename = "ratings_" + teacher.getNameSubject();

        //Veci na vytvorenie pdfka
        Document document = new Document();
        FileOutputStream fileOutputStream = new FileOutputStream(path + filename + ".pdf");
        PdfWriter.getInstance(document, fileOutputStream);
        document.open();
        //Toto som sem musel dat lebo si to myslelo ze dokument je prazdny
        document.add(new Chunk(""));

        while (it.hasNext()) {

            //Vezmem value teda array frekvencii hodnoteni a nazov predmetu z objektu na ktoroj sme teraz
            Map.Entry pair = (Map.Entry) it.next();
            int[] frequencies = (int[]) pair.getValue();
            String question = (String) pair.getKey();

            //Vytvorim dataset na graf
            DefaultCategoryDataset dataSet = new DefaultCategoryDataset();

            //Vytvorim sablonu tabulky
            PdfPTable table = new PdfPTable(6);
            table.addCell("Hodnota/ value");

            //Pridam to tabulky cisla hodnoteni
            for (int i = 1; i <= frequencies.length; i++) {
                table.addCell(Integer.toString(i));
            }

            table.addCell("Koľkokrát/ frequency");

            //Intermediate premenne na priemer
            int totalScore = 0;
            int totalRespondents = 0;
            float average = 0;

            //Prebehnem pole s frekvenciami a dam ich do datasetu
            for (int i = 0; i < frequencies.length; i++) {

                //Pridam frekvencie do datasetu na fraf
                dataSet.setValue(frequencies[i], "Frequency", Integer.toString(i + 1));
                //Pridam frekvencie do tabulky
                table.addCell(Integer.toString(frequencies[i]));

                totalScore += (i + 1) * frequencies[i];
                totalRespondents += frequencies[i];

            }

            //Vyratam priemer
            average = (float) totalScore / (float) totalRespondents;
            //A pridam ho do tabulky
            table.addCell("Priemer/ average");
            table.addCell(Float.toString(average));
            //Doplnim prazdne bunky do riadku s priemerom lebo inak to nechce spravit tabulku
            for (int i = 2; i <= frequencies.length; i++) {
                table.addCell("");
            }

            //Toto tu zevraj treba podla StackOverflow
            it.remove();

            //Spravim chart objekt s datasetu
            JFreeChart chart = ChartFactory.createBarChart(
                    "Vaše hodnotenie/ your evaluation", "Hodnota/ value", "Koľkokrát/ frequency",
                    dataSet, PlotOrientation.VERTICAL, false, true, false);

            //A nakreslim z toho pekny obrazok
            String chartFileName = "chart" + Integer.toString(filenumber) + ".png";
            File barChart = new File(chartFileName);
            ChartUtils.saveChartAsPNG(barChart, chart, 640, 480);

            //Dam otazku a za nou graf a tabulku to pdfka
            Image chartImage = Image.getInstance(chartFileName);
            chartImage.scalePercent(75);
            document.add(new Paragraph(question));
            document.add(chartImage);
            document.add(table);
            document.newPage();

            //Zvysim cislo grafu o 1
            filenumber++;

        }

        //Koniec prace s pdfkom
        document.close();
        fileOutputStream.close();

    }

    public HashMap calculateAverages(Vector teachers) {

        HashMap<String, LinkedList<Float>> averages = new HashMap<>();

        for (Object teacher : teachers) {

            HashMap ratings = ((Teacher) teacher).getHashMap();
            Iterator it = ratings.entrySet().iterator();

            while (it.hasNext()) {

                Map.Entry pair = (Map.Entry) it.next();
                int[] frequencies = (int[]) pair.getValue();
                String question = (String) pair.getKey();

                int totalScore = 0;
                int totalRespondents = 0;
                float currentAverage = 0;

                for (int i = 0; i < frequencies.length; i++) {

                    totalScore += (i + 1) * frequencies[i];
                    totalRespondents += frequencies[i];

                }

                currentAverage = (float) totalScore / (float) totalRespondents;

                if (averages.containsKey(question)) {

                    LinkedList temp = averages.get(question);
                    addValue(temp, currentAverage);
                    averages.put(question, temp);

                } else {

                    LinkedList<Float> temp = new LinkedList<>();
                    addValue(temp, currentAverage);
                    averages.put(question, temp);

                }

                it.remove();
            }

        }

        return averages;

    }

    private static void addValue(LinkedList<Float> llist, float val) {

        if (llist.size() == 0) {
            llist.add(val);
        } else if (llist.get(0) > val) {
            llist.add(0, val);
        } else if (llist.get(llist.size() - 1) < val) {
            llist.add(llist.size(), val);
        } else {
            int i = 0;
            while (llist.get(i) < val) {
                i++;
            }
            llist.add(i, val);
        }

    }

}

class Teacher {
    private String nameSubject;
    private HashMap<String, int[]> numQuestions;
    private Vector<String> subjectEval, teacherEval;

    Teacher(String nameSubject){
        this.nameSubject = nameSubject;
        numQuestions = new HashMap<>();
        subjectEval = new Vector<>(5,5);
        teacherEval = new Vector<>(5,5);
    }

    void addScore(String question, double score, double maxScore){
        if (numQuestions.containsKey(question)){
            int[] frequency = numQuestions.get(question);
            frequency[(int)(score-1)]+=1;
            numQuestions.replace(question, frequency);
        }
        else{
            int[] frequency = new int[(int)maxScore];
            for (int i=0; i<maxScore; i++){
                frequency[i]=0;
            }
            frequency[(int)(score-1)]+=1;
            numQuestions.put(question, frequency);
        }
    }

    void addSubjectEval(String evaluation){
        subjectEval.add(evaluation);
    }

    String[] getSubjectEval(){
        return subjectEval.toArray(new String[0]);
    }

    void addTeacherEval(String evaluation){
        teacherEval.add(evaluation);
    }

    String[] getTeacherEval(){
        return teacherEval.toArray(new String[0]);
    }

    String getNameSubject(){
        return nameSubject;
    }

    HashMap getHashMap() {
        return numQuestions;
    }
}
