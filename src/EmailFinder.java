import static javax.swing.JOptionPane.*;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class EmailFinder implements Runnable {
    public static void main(String[] args) {
        new EmailFinder("Mailing_list", "csv", EMAIL_SPLITTER).run();
        System.exit(0);
    }

    static final BiFunction<TextConfig, Pattern, List<String>> FINDER = TextConfig::find;

    static final Function<TextConfig, List<String>>
            EMAIL_SPLITTER = textConf -> FINDER.apply(textConf, WebPageReader.EMAIL_REGEX),
            NAME_SPLITTER = textConf -> FINDER.apply(textConf, WebPageReader.NAME_REGEX);

    final Function<TextConfig, List<String>>[] splitters;
    final TextConfig record;
    final JFrame window;
    final JProgressBar progress;
    int pv = 0;

    @SafeVarargs
    EmailFinder(String fileName, String fileExt, Function<TextConfig, List<String>>... patternMatchers) {
        record = new TextConfig(fileName+'.'+fileExt);
        this.splitters = patternMatchers.length > 0 ? patternMatchers : new Function[]{EMAIL_SPLITTER};
        window = new JFrame("E-mail Finder");
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int) screenSize.getWidth();
        int height = (int) screenSize.getHeight();
        window.setLocation(width/4, height/3);
        window.setSize(350, 95);
        progress = new JProgressBar(SwingConstants.HORIZONTAL, 0, 3);
        progress.setStringPainted(true);
        progress.setMaximum(5);
        window.add(progress);
        progress.setString("Initialising...");
        progress.setValue(pv);
    }

    @Override
    public void run() {
        try {
            progress.setVisible(true);
            window.setVisible(true);
            Object[] options = {"File", "Web", "List of URLs"};
            progress.setValue(++pv);
            progress.setString("Acquiring documents...");

            switch (showOptionDialog(window, "Would you like to search a file or online web page?", "Query type", YES_NO_CANCEL_OPTION, QUESTION_MESSAGE, null, options, options[1])) {
                case (YES_OPTION):
                    extractFromFile();
                    break;
                case NO_OPTION:
                    extractFromWebPage();
                    break;
                case CANCEL_OPTION:
                    extractFromURLs();
                    break;
                default:
                    System.exit(0);
            }


            if (!record.toString().isEmpty()) {
                progress.setString("Writing to file...");
                progress.setValue(++pv);
                record.save();
                showMessageDialog(window, "Done! You can find the results on your desktop", "Finished search", INFORMATION_MESSAGE);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            showMessageDialog(window, ex.getClass().getName()+": "+ex.getMessage(), "Oops! Something went wrong...", ERROR_MESSAGE);
        }
    }

    private void extractFromFile() {
        Set<File> pages = getFiles();
        if (pages.size() < 1)
            return;

        progress.setMaximum(pages.size() + 4);
        for (File page : pages) {
            progress.setValue(++pv);
            String mult = pages.size() > 1 ? "" + pv : "";
            progress.setString("Accessing file " + mult + "...");
            findAndWrite(accessHandler(page), "For file \"" + page.getName() + "\":");
        }
    }

    private void extractFromWebPage() {
        Set<WebPageReader> sites = getSites();
        progress.setMaximum((sites.size() * 2) + 2);
        for (WebPageReader site : sites) {
            progress.setValue(++pv);
            String mult = sites.size() > 1 ? "" + pv : "";
            progress.setString("Connecting to site " + mult + "...");
            try {
                site.read();
                findAndWrite(site.getEditor(), "For site \"" + site.getURL() + "\":");
            }
            catch (IOException ioe) {
                showMessageDialog(window, ioe.getMessage(), "Could not read site!", ERROR_MESSAGE);
                ++pv;
            }
        }
    }

    private void extractFromURLs() {
        File file = getFiles().iterator().next();
        TextConfig editor = accessHandler(file);
        progress.setString("Finding URLs...");
        progress.setValue(++pv);
        assert editor != null;
        Collection<String> links = editor.find(WebPageReader.WEBSITE_REGEX);
        progress.setMaximum((links.size() * 3) + 2);
        for (String link : links) {
            progress.setValue(++pv);
            progress.setString("Connecting to " + link + "...");
            WebPageReader connector;
            try {
                connector = new WebPageReader(link);
                connector.read();
            }
            catch (IOException iox) {
                progress.setString("Could not read web page!");
                pv += 2;
                progress.setValue(pv);
                continue;
            }
            findAndWrite(connector.getEditor(), String.format("For site \"%s\":", link));
        }
    }

    private void findAndWrite(TextConfig textConf, String before) {
        appendResults(before, search(textConf));
    }

    private List<String> search(TextConfig textConf) {
        progress.setString("Searching...");
        progress.setValue(++pv);
        List<String> results = new ArrayList<>();
        for (Function<TextConfig, List<String>> splitter : splitters) {
            results.addAll(splitter.apply(textConf));
        }
        progress.setString(results.size() > 0 ? "Saving..." : "None found for this document!");
        return results;
    }

    private void appendResults(String before, List<String> lines) {
        progress.setValue(++pv);
        if (before != null) {
            record.append(before);
        }
        record.insertNewLines(1);
        lines.forEach(email -> record.append(email + ", " + TextConfig.LINE_SEPARATOR));
        record.insertNewLines(2);
    }

    static TextConfig getAccess(File page) throws IOException {
        TextConfig editor;
        if (TextConfig.getExtension(page.getName()).equalsIgnoreCase("PDF")) {
            PDFParser parser = new PDFParser(new org.apache.pdfbox.io.RandomAccessFile(page, "rw"));
            parser.parse();
            COSDocument cosDoc = parser.getDocument();
            PDFTextStripper pdfStripper = new PDFTextStripper();
            PDDocument pdDoc = new PDDocument(cosDoc);
            pdfStripper.setStartPage(1);
            pdfStripper.setEndPage(pdDoc.getNumberOfPages());
            String pdfStr = pdfStripper.getText(pdDoc);

            editor = new TextConfig();
            editor.overwrite(pdfStr);
        }
        else {
            editor = new TextConfig(page);
            editor.update();
        }
        return editor;
    }

    private TextConfig accessHandler(File page) {
        try {
            return getAccess(page);
        }
        catch (IOException ioe) {
            showMessageDialog(window, "Couldn't read the file. Please try again or copy the text contents into a plain text file.", ioe.getMessage(), ERROR_MESSAGE);
        }
        return null;
    }

    private File promptForFiles(JFileChooser fc) {
        File file = null;
        while (fc.showOpenDialog(window) == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
            if (fc.getFileFilter().accept(file)) {
                if (!file.exists()) {
                    showMessageDialog(window, "File does not exist! Please try again.", "Invalid file", ERROR_MESSAGE);
                    continue;
                }
                if (!file.canRead()) {
                    showMessageDialog(window, "File does not allow read permission. Please select another file.", "Invalid file", ERROR_MESSAGE);
                    continue;
                }
                if (file.length() < 4) {
                    showMessageDialog(window, "The selected file is empty! Please choose another file.", "Invalid file", ERROR_MESSAGE);
                    continue;
                }
                break;
            }
            else
                showMessageDialog(window, "Invalid file format. Please choose a text file.", "Invalid file", ERROR_MESSAGE);
            if (file == null)
                break;
        }
        return file;
    }

    Set<File> getFiles() {
        JFileChooser fc = new JFileChooser(System.getProperty("user.home"));
        FileNameExtensionFilter text = new FileNameExtensionFilter("Plain text, HTML, PDF", "text", "txt", "csv", "html", "htm", "pdf");
        fc.setFileFilter(text);
        Set<File> pages = new HashSet<>();
        do {
            File selected = promptForFiles(fc);
            if (selected != null)
                pages.add(selected);
            else
                break;
        }
        while (showOptionDialog(window, "Would you like to add more files?", "Additional URLs", YES_NO_OPTION, QUESTION_MESSAGE, null, null, null) == YES_OPTION);
        return pages;
    }

    Set<WebPageReader> getSites() {
        Set<WebPageReader> sites = new HashSet<>();
        while (true) {
            String response = showInputDialog(window, "Please enter the URL of a site.", "http://");
            if (response == null || response.trim().length() < 10)
                break;
            try {
                sites.add(new WebPageReader(response));
                if (showOptionDialog(window, "Would you like to add more sites?", "Additional URLs", YES_NO_OPTION, QUESTION_MESSAGE, null, null, null) == NO_OPTION)
                    break;
            }
            catch (Exception invalid) {
                showMessageDialog(window, "Please enter the full URL, including the protocol", "Invalid URL", ERROR_MESSAGE);
            }
        }
        return sites;
    }

}
