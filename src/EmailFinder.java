import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static javax.swing.JOptionPane.*;

public class EmailFinder
{
    public static void main (String[] args)
    {
        new EmailFinder().run();
        System.exit(0);
    }

    JFrame window;
    JProgressBar progress;
    TextConfig record;
    int pv;

    private void initialise()
    {
        pv = 0;
        window = new JFrame ("E-mail Finder");
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int)screenSize.getWidth();
        int height = (int)screenSize.getHeight();
        window.setLocation(width/4, height/3);
        window.setSize(new Dimension(350, 95));
        progress = new JProgressBar(SwingConstants.HORIZONTAL, 0, 3);
        progress.setStringPainted(true);
        progress.setMaximum(5);
        window.add(progress);
        progress.setString("Initialising...");
        progress.setValue(pv);
        record = new TextConfig("Mailing_list.csv");
        progress.setVisible(true);
        window.setVisible(true);
    }

    public TextConfig run()
    {
        initialise();
        Object[] options = {"File", "Web", "List of URLs"};
        progress.setValue(++pv);
        progress.setString("Acquiring documents...");
        int type = showOptionDialog(window, "Would you like to search a file or online web page?", "Query type", YES_NO_CANCEL_OPTION, QUESTION_MESSAGE, null, options, options[1]);

        if (type == YES_OPTION)
        {
            Set<File> pages = getFiles();
            if (pages.size() < 1)
                System.exit(0);

            progress.setMaximum(pages.size() + 4);
            for (File page : pages)
            {
                progress.setValue(++pv);
                String mult = pages.size() > 1 ? ""+pv : "";
                progress.setString("Accessing file " + mult + "...");
                TextConfig editor;
                while (true)
                {
                    try
                    {
                        editor = getAccess (page);
                        break;
                    }
                    catch (IOException ioe)
                    {
                        showMessageDialog(window, "Couldn't read the file. Please try again or copy the text contents into a plain text file.", ioe.getMessage(), ERROR_MESSAGE);
                    }
                }
                progress.setString("Searching for e-mail addresses...");
                progress.setValue(++pv);
                Set<String> address = editor.find(WebPageReader.emailRegex);
                progress.setString(address.size() > 0 ? "Saving addresses..." : "No addresses found for this file!");
                progress.setValue(++pv);
                if (pages.size() > 1)
                {
                    record.append("For file \""+page.getName()+"\":");
                    record.insertNewLines(1);
                }
                address.forEach(email -> record.append(email+'\n'));
                record.insertNewLines(2);
            }
        }

        else if (type == NO_OPTION)
        {
            Set<WebPageReader> sites = getSites();
            progress.setMaximum((sites.size()*2)+2);
            for (WebPageReader site : sites)
            {
                progress.setValue(++pv);
                String mult = sites.size() > 1 ? ""+pv : "";
                progress.setString("Connecting to site " + mult + "...");
                try
                {
                    site.read();
                    progress.setString("Searching for e-mail addresses...");
                    progress.setValue(++pv);
                    Set<String> address = site.getEditor().find(WebPageReader.emailRegex);
                    progress.setString(address.size() > 0 ? "Saving addresses..." : "No addresses found for this site!");
                    progress.setValue(++pv);
                    record.append("For site \""+site.getURL()+"\":");
                    record.insertNewLines(1);
                    address.forEach(email -> record.append(email+", "));
                    record.insertNewLines(2);
                } catch (IOException ioe)
                {
                    showMessageDialog(window, ioe.getMessage(), "Could not read site!", ERROR_MESSAGE);
                    ++pv;
                }
            }
        }

        else if (type == CANCEL_OPTION)
        {
            File[] files = getFiles(1).toArray(new File[1]);
            if (files[0] == null)
                System.exit(0);
            TextConfig editor = accessHandler(files[0]);
            progress.setString("Finding URLs...");
            progress.setValue(++pv);
            Set<String> links = editor.find(WebPageReader.websiteRegex);
            progress.setMaximum((links.size()*3)+2);
            for (String link : links)
            {
                progress.setValue(++pv);
                progress.setString("Connecting to " + link + "...");
                WebPageReader connector;
                try
                {
                    connector = new WebPageReader(link);
                    connector.read();
                }
                catch (IOException e)
                {
                    progress.setString("Could not read web page!");
                    pv += 2;
                    progress.setValue(pv);
                    continue;
                }
                progress.setString("Searching for e-mail addresses...");
                progress.setValue(++pv);
                Set<String> address = connector.getEditor().find(WebPageReader.emailRegex);
                progress.setString(address.size() > 0 ? "Saving addresses..." : "No addresses found for this file!");
                progress.setValue(++pv);
                record.append("For site \"" + link + "\":");
                record.insertNewLines(1);
                address.forEach(email -> record.append(email+", "));
                record.insertNewLines(2);
            }
        }
        else
            System.exit(0);

        progress.setString("Writing to file...");
        progress.setValue(++pv);
        record.save();
        showMessageDialog(window, "Done! You can find the results on your desktop", "Finished search", INFORMATION_MESSAGE);
        return record;
    }

    static TextConfig getAccess (File page) throws IOException
    {
        TextConfig editor;
        if (TextConfig.getExtension(page.getName()).equalsIgnoreCase("PDF"))
        {
            PDFParser parser = new PDFParser(new FileInputStream(page));
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
        else
        {
            editor = new TextConfig(page);
            editor.update();
        }
        return editor;
    }

    private TextConfig accessHandler (File page)
    {
        TextConfig editor;
        while (true)
        {
            try
            {
                editor = getAccess(page);
                break;
            }
            catch (IOException ioe)
            {
                showMessageDialog(window, "Couldn't read the file. Please try again or copy the text contents into a plain text file.", ioe.getMessage(), ERROR_MESSAGE);
            }
        }
        return editor;
    }

    private File promptForFiles (JFileChooser fc)
    {
        File file = null;
        while (fc.showOpenDialog(window) == JFileChooser.APPROVE_OPTION)
        {
            file = fc.getSelectedFile();
            if (fc.getFileFilter().accept(file))
            {
                if (!file.exists())
                {
                    showMessageDialog(window, "File does not exist! Please try again.", "Invalid file", ERROR_MESSAGE);
                    continue;
                }
                if (!file.canRead())
                {
                    showMessageDialog(window, "File does not allow read permission. Please select another file.", "Invalid file", ERROR_MESSAGE);
                    continue;
                }
                if (file.length() < 4)
                {
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

    Set<File> getFiles (int howMany)
    {
        if (howMany < 1)
            return getFiles();
        String home = System.getProperty("user.home");
        JFileChooser fc = new JFileChooser(home);
        FileNameExtensionFilter text = new FileNameExtensionFilter("Plain text, HTML, PDF", "text", "txt", "csv", "html", "htm", "pdf");
        fc.setFileFilter(text);
        Set<File> pages = new HashSet<>();
        int attempts = 0;
        while (attempts < howMany)
        {
            ++attempts;
            File selected = null;
            while (selected == null)
            {
                selected = promptForFiles(fc);
                if (selected == null)
                    showMessageDialog(window,"You must choose a file.","No file selected",ERROR_MESSAGE);
            }
            pages.add(selected);
        }
        return pages; //.toArray(new File[pages.size()]);
    }

    Set<File> getFiles()
    {
        String home = System.getProperty("user.home");
        JFileChooser fc = new JFileChooser(home);
        FileNameExtensionFilter text = new FileNameExtensionFilter("Plain text, HTML, PDF", "text", "txt", "csv", "html", "htm", "pdf");
        fc.setFileFilter(text);
        Set<File> pages = new HashSet<>();
        do
        {
            File selected = promptForFiles(fc);
            if (selected != null)
                pages.add(selected);
            else
                break;
        }
        while (showOptionDialog(window, "Would you like to add more files?", "Additional URLs", YES_NO_OPTION, QUESTION_MESSAGE, null, null, null) == YES_OPTION);
        return pages; //.toArray(new File[pages.size()]);
    }

    Set<WebPageReader> getSites()
    {
        Set<WebPageReader> sites = new HashSet<>();
        while (true)
        {
            String response = showInputDialog(window, "Please enter the URL of a site.", "http://");
            if (response == null || response.trim().length() < 10)
                break;
            try
            {
                sites.add (new WebPageReader(response));
                if (showOptionDialog(window, "Would you like to add more sites?", "Additional URLs", YES_NO_OPTION, QUESTION_MESSAGE, null, null, null) == NO_OPTION)
                    break;
            }
            catch (MalformedURLException invalid)
            {
                showMessageDialog(window, "Please enter the full URL, including the protocol", "Invalid URL", ERROR_MESSAGE);
            }
        }
        return sites;
    }

}
