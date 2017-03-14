import javax.swing.*;
import static javax.swing.JOptionPane.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;

public class ChangeNotifier
{
    final LinkedHashSet<Tag> dynamic = new LinkedHashSet<>();
    private TextConfig record;
    private final String appname, interval, website, converter, htmli;
    private final SiteSearchSyntax autosearch;
    private final double thold;
    private final long lag;
    private AssetMonitor stock;
    private Scheduler plan;
    JButton addButton, rmButton, scheduleButton, targButton;
    JList<String> itemList;
    JScrollPane scroller;
    JPanel panel;
    final JFrame window;

    public ChangeNotifier (String appName, String site, SiteSearchSyntax surl, String HTMLindex, String convertCurrency, String interval, double deviation, long delay)
    {
        window = new JFrame(appName);
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int)screenSize.getWidth();
        int height = (int)screenSize.getHeight();
        window.setLocation(width/7, height/4);
        this.appname = appName;
        this.website = site;
        this.autosearch = surl;
        this.converter = convertCurrency;
        this.thold = deviation;
        this.htmli = HTMLindex;
        this.interval = interval;
        this.lag = delay;
    }

    public synchronized void start (boolean useProgress) throws RuntimeException, NoClassDefFoundError
    {
        if (useProgress)
        {
            JProgressBar loading = new JProgressBar (SwingConstants.HORIZONTAL,0,7);
            window.setSize(300,100);
            window.add(loading);
            window.setVisible(true);
            loading.setValue(0);
            loading.setStringPainted(true);
            loading.setVisible(true);
            loading.setString("Acquiring items...");
            loading.setValue(1);
            this.setup();
            loading.setString("Initialising assets...");
            loading.setValue(2);
            stock = new AssetMonitor(dynamic.clone(), record, lag, null, itemList, false, loading, autosearch);
            setThreshold();
            loading.setString("Clearing file...");
            loading.setValue(3);
            String temp = record.read();
            WindowAdapter restore = new WindowAdapter()
            {
                @Override
                public void windowClosing(WindowEvent windowEvent)
                {
                    if (JOptionPane.showConfirmDialog(window, "Are you sure to close the program?", "Quit "+appname, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)
                    {
                        record.overwrite(temp);
                        record.save();
                        System.exit(0);
                    }
                }
            };
            window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            window.addWindowListener(restore);
            record.eraseContents();
            loading.setString("Updating assets...");
            loading.setValue(4);
            stock.Trigger("Initialize");
            loading.setString("Setting schedule...");
            loading.setValue(5);
            window.removeWindowListener(restore);
            window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            plan = new Scheduler(stock);
            plan.setInterval(interval);
            loading.setString("Your items are being monitored...");
            loading.setValue(6);
            plan.initiate();
            loading.setValue(7);
            loading.setString("All done!");
            window.remove(loading);
            useGUI();
        }
        else
        {
            this.setup();
            window.setVisible(false);
            stock = new AssetMonitor(dynamic.clone(), record, lag, null, itemList, false, null, autosearch);
            setThreshold();
            String temp = record.read();
            WindowAdapter restore = new WindowAdapter()
            {
                @Override
                public void windowClosing(WindowEvent windowEvent)
                {
                    if (JOptionPane.showConfirmDialog(window, "Are you sure to close the program?", "Quit "+appname, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)
                    {
                        record.overwrite(temp);
                        record.save();
                        System.exit(0);
                    }
                }
            };
            window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            window.addWindowListener(restore);
            record.eraseContents();
            stock.Trigger("Initialize");
            window.removeWindowListener(restore);
            window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            plan = new Scheduler(stock);
            plan.setInterval(interval);
            window.setVisible(true);
            showMessageDialog(window, "Your items are being monitored...", appname, INFORMATION_MESSAGE);
            plan.initiate();
            useGUI();
        }
    }

    private void setup()
    {
        boolean fromFile = false;
        String f = System.getProperty("file.separator");
        String home = System.getProperty("user.home");
        File file = new File (home+f+"Desktop"+f+appname+".txt");
        window.setVisible(true);
        if (file.exists() && file.canRead() && file.canWrite() && file.length() > 8)
            fromFile = true;
        else
        {
            JFileChooser fc = new JFileChooser(home);
            FileNameExtensionFilter text = new FileNameExtensionFilter("Text files", "txt", "text");
            fc.setFileFilter(text);
            while (fc.showOpenDialog(window) == JFileChooser.APPROVE_OPTION)
            {
                file = fc.getSelectedFile();
                if (text.accept(file))
                {
                    if (!file.exists())
                    {
                        showMessageDialog(window, "File does not exist! Please try again, or press 'Cancel' to enter the items manually.", "Invalid file", ERROR_MESSAGE);
                        continue;
                    }
                    if (!(file.canRead() || file.canWrite()))
                    {
                        showMessageDialog(window, "File does not allow read/write permission. Please select another file, or press 'Cancel' to enter the items manually.", "Invalid file", ERROR_MESSAGE);
                        continue;
                    }
                    if (file.length() < 4)
                    {
                        showMessageDialog(window, "The selected file is empty! Please choose another file, or press 'Cancel' to enter the items manually.", "Invalid file", ERROR_MESSAGE);
                        continue;
                    }
                    fromFile = true;
                    /*String name = file.getPath();
                    name = name.substring (0, name.lastIndexOf(f)+f.length());
                    file.renameTo (new File(name));*/
                    break;
                }
                else
                    showMessageDialog(window, "Invalid file format. Please choose a text file, or press 'Cancel' to enter the items manually.", "Invalid file", ERROR_MESSAGE);
            }
        }

        if (fromFile)
        {
            record = new TextConfig(file);
            String[] lines = record.readAllNonBlankLines();

            for (String line : lines)
            {
                Tag tag;
                if (line.startsWith("Item ") && (line.contains("Name: \"")))
                {
                    tag = new Tag();
                    tag.fromString(line);
                }
                else if (line.contains(website))
                {
                    String url = line.substring(line.indexOf(website));
                    tag = new Tag();
                    tag.setName (url.trim());
                }
                else if (!WebPageReader.isValidURL(line) && autosearch != null)
                {
                    tag = new Tag();
                    tag.setName(autosearch.query(line).toString());
                }
                else
                    continue;

                tag.setProperties(htmli);
                tag.setDescription(converter);
                dynamic.add(tag);
            }
        }
        else
        {
            addManually(true);
            record = new TextConfig(appname);
        }
    }

    private void addManually (boolean initial)
    {
        boolean more;
        String question = autosearch != null ? "Please enter the name or URL of the item you'd like to add" : "Please enter the URL of the item you'd like to add";
        do
        {
            String response = showInputDialog(window,question);
            if (response == null && initial)
                System.exit(0);
            else if (response == null)
                break;

            Tag tag;
            if (response.toLowerCase().startsWith("http") && WebPageReader.isValidURL(response) && response.contains(website))
            {
                tag = new Tag();
                tag.setName(response);
            }
            else if (autosearch != null && response.length() > 0)
            {
                tag = new Tag();
                tag.setName(autosearch.query(response).toString());
            }
            else
            {
                showMessageDialog(window, "Please enter a valid URL, including the protocol (http://)", "Invalid URL", ERROR_MESSAGE);
                more = true;
                continue;
            }
            tag.setProperties(htmli);
            tag.setDescription(converter);
            dynamic.add(tag);
            if (!initial)
                stock.addItem(tag);
            more = showConfirmDialog(window, "Would you like to add more items?", "Item added", YES_NO_OPTION, QUESTION_MESSAGE) == YES_OPTION;
        }
        while (more);
    }

    private void useGUI()
    {
        window.setSize(720,360);
        panel.setSize(window.getSize());
        window.add(panel);
        panel.setVisible(true);

        addButton.addActionListener (e -> {
            plan.terminate();
            addManually(false);
            record.save();
            plan.initiate();
            showMessageDialog(window, "Your items are being monitored.", "Success!", INFORMATION_MESSAGE);
        });

        rmButton.addActionListener (rma -> {
            if (itemList.getSelectedIndex() >= 0)
            {
                String question = "Are you sure you want to remove ";
                List<String> values = itemList.getSelectedValuesList();
                question += values.size() > 1 ? "these items?" : "this item?";
                String msg = values.size() > 1 ? "Items removed." : "Item removed.";
                if (showConfirmDialog(window, question, "Confirm delete", YES_NO_OPTION) == YES_OPTION)
                {
                    plan.terminate();
                    for (String value : values)
                    {
                        if (value != null)
                        {
                            Tag selected = new Tag(0);
                            selected.fromString(value);
                            int number;
                            try
                            {
                                number = Integer.parseInt(record.getFieldFor((String) selected.getName()).substring("Item ".length()));
                            } catch (StringIndexOutOfBoundsException nt)
                            {
                                number = -1;
                                showMessageDialog(window, "Invalid selection", "Could not remove this item. Please try again later.", ERROR_MESSAGE);
                            }
                            if (number > 0)
                                record.remove("Item "+number);
                            Tag deleted = dynamic.stream().filter(tag -> tag.getName().equals(selected.getName())).findFirst().get();
                            /*try {
                                Thread.sleep((long) stock.getDelay());
                            } catch (InterruptedException ignored){}*/
                            dynamic.remove(deleted);
                            record.eraseContents();
                            stock.removeItem(deleted);
                        }
                        else
                            showMessageDialog(window, "Invalid selection.", "No item", ERROR_MESSAGE);
                    }
                    plan.initiate();
                    showMessageDialog(window, msg, "Success!", INFORMATION_MESSAGE);
                }
            }
        });

        targButton.addActionListener(bp -> {
            List<String> values = itemList.getSelectedValuesList();
            String s = values.size() > 0 ? "s" : "";
            Object[] options = {"Low threshold", "High threshold", "Low target", "High target", "Cancel"};
            String qs = "Threshold changes % deviation rule, Target is for nominal values of individual items.";
            switch (showOptionDialog(window, qs, targButton.getName(), OK_CANCEL_OPTION, INFORMATION_MESSAGE, null, options, options[0]))
            {
                case 0:
                {
                    plan.terminate();
                    double low = -1;
                    while (low < 0) try
                    {
                        low = Double.parseDouble(showInputDialog(window, "Enter a (decimal) low threshold", stock.getLowThreshold()));
                    }
                    catch (NumberFormatException NaN)
                    {
                        showMessageDialog(window, "Please enter a decimal value.", "Invalid number", ERROR_MESSAGE);
                    }
                    catch (NullPointerException cancelled)
                    {
                        break;
                    }
                    stock.setLowThreshold(low);
                    plan.initiate();
                    showMessageDialog(window, "Low threshold has been updated", "Success!", INFORMATION_MESSAGE);
                    break;
                }
                case 1:
                {
                    plan.terminate();
                    double high = -1;
                    while (high < 0) try
                    {
                        high = Double.parseDouble(showInputDialog(window, "Enter a (decimal) high threshold", stock.getHighThreshold()));
                    }
                    catch (NumberFormatException NaN)
                    {
                        showMessageDialog(window, "Please enter a positive decimal value.", "Invalid number", ERROR_MESSAGE);
                    }
                    catch (NullPointerException cancelled)
                    {
                        break;
                    }
                    stock.setHighThreshold(high);
                    plan.initiate();
                    showMessageDialog(window, "High threshold has been updated", "Success!", INFORMATION_MESSAGE);
                    break;
                }
                case 2: //Low target
                {
                    plan.terminate();
                    if (values.size() < 1)
                        showMessageDialog(window, "Please select at least one item.", "No items selected", ERROR_MESSAGE);
                    else
                    {
                        double target = -1;
                        while (target < 0) try
                        {
                            target = Double.parseDouble(showInputDialog(window, "Enter a nominal low target value for the selected item"+s, stock.getHighThreshold()));
                        }
                        catch (NumberFormatException NaN)
                        {
                            showMessageDialog(window, "Please enter a positive decimal value.", "Invalid number", ERROR_MESSAGE);
                        }
                        catch (NullPointerException cancelled)
                        {
                            plan.initiate();
                            break;
                        }
                        if (target < 0)
                            break;
                        Double[] lowTargs = stock.getLowTarget();
                        for (String value : values)
                        {
                            Tag selected = new Tag(0);
                            selected.fromString(value);
                            int index;
                            try {
                                index = Integer.parseInt(record.getFieldFor((String) selected.getName()).substring("Item ".length()));
                            }
                            catch (NullPointerException npe)
                            {
                                showMessageDialog(window, "Could not update target for this item.", "Error encountered", ERROR_MESSAGE);
                                plan.initiate();
                                break;
                            }
                            if (index > 0 && index <= dynamic.size())
                                lowTargs[index-1] = target;
                        }
                        stock.setLowTarget(lowTargs);
                        String plural = s.isEmpty() ? " has" : "s have";
                        plan.initiate();
                        showMessageDialog(window, "Low target"+plural+" been set for the selected item"+s, "Success!", INFORMATION_MESSAGE);
                    }
                    break;
                }
                case 3: //High Target
                {
                    plan.terminate();
                    if (values.size() < 1)
                        showMessageDialog(window, "Please select at least one item.", "No items selected", ERROR_MESSAGE);
                    else
                    {
                        double target = -1;
                        while (target < 0) try
                        {
                            target = Double.parseDouble(showInputDialog(window, "Enter a nominal high target value for the selected item"+s, stock.getHighThreshold()));
                        }
                        catch (NumberFormatException NaN)
                        {
                            showMessageDialog(window, "Please enter a positive decimal value.", "Invalid number", ERROR_MESSAGE);
                        }
                        catch (NullPointerException cancelled)
                        {
                            plan.initiate();
                            break;
                        }
                        if (target < 0)
                            break;
                        Double[] highTargs = stock.getHighTarget();
                        for (String value : values)
                        {
                            Tag selected = new Tag(0);
                            selected.fromString(value);
                            int index;
                            try {
                                index = Integer.parseInt(record.getFieldFor((String) selected.getName()).substring("Item ".length()));
                            }
                            catch (NullPointerException npe)
                            {
                                showMessageDialog(window, "Could not update target for this item.", "Error encountered", ERROR_MESSAGE);
                                continue;
                            }
                            if (index > 0 && index <= dynamic.size())
                                highTargs[index-1] = target;
                        }
                        stock.setHighTarget(highTargs);
                        String plural = s.isEmpty() ? " has" : "s have";
                        plan.initiate();
                        showMessageDialog(window, "High target"+plural+" been set for the selected item"+s, "Success!", INFORMATION_MESSAGE);
                    }
                    break;
                }
            }
        });

        scheduleButton.addActionListener (cs -> {
            Object[] options = {"Interval", "Offset", "Start", "End", "Days of week", "Cancel"};
            int choice = showOptionDialog(window, "Which aspects would you like to edit?", "Modify schedule", DEFAULT_OPTION, QUESTION_MESSAGE, null, options, options[0]);
            if (choice < 5)
            {
                plan.terminate();
                switch (choice)
                {
                    case 0:
                    {
                        String response = showInputDialog(window, "Please enter the desired interval");
                        while (response != null && !Scheduler.isValidTime(response))
                            response = showInputDialog(window, "Invalid entry. Please enter the desired interval", "", ERROR_MESSAGE);
                        if (Scheduler.isValidTime(response))
                        {
                            plan.setInterval(response);
                            showMessageDialog(window, "Interval has been set", "Success!", INFORMATION_MESSAGE);
                        }
                        break;
                    }
                    case 1:
                    {
                        String response = showInputDialog(window, "Please enter the desired offset");
                        while (response != null && !Scheduler.isValidTime(response))
                            response = showInputDialog(window, "Invalid entry. Please enter the desired offset", "", ERROR_MESSAGE);
                        if (Scheduler.isValidTime(response))
                        {
                            plan.setOffset(response);
                            showMessageDialog(window, "Offset has been set", "Success!", INFORMATION_MESSAGE);
                        }
                        break;
                    }
                    case 2:
                    {
                        String response = showInputDialog(window, "Please enter the desired start time");
                        while (response != null && !Scheduler.isValidTime(response))
                            response = showInputDialog(window, "Invalid entry. Please enter the start time", "", ERROR_MESSAGE);
                        if (Scheduler.isValidTime(response))
                        {
                            plan.setStart(response);
                            showMessageDialog(window, "Start time has been set", "Success!", INFORMATION_MESSAGE);
                        }
                        break;
                    }
                    case 3:
                    {
                        String response = showInputDialog(window, "Please enter the desired end time");
                        while (response != null && !Scheduler.isValidTime(response))
                            response = showInputDialog(window, "Invalid entry. Please enter the desired end time", "", ERROR_MESSAGE);
                        if (Scheduler.isValidTime(response))
                        {
                            plan.setEnd(response);
                            showMessageDialog(window, "End time has been set", "Success!", INFORMATION_MESSAGE);
                        }
                        break;
                    }
                    case 4:
                    {
                        String response = showInputDialog(window, "Please enter the desired days of week");
                        while (response != null && !Scheduler.isValidDay(response))
                            response = showInputDialog(window, "Invalid entry. Please enter the desired days of week", "", ERROR_MESSAGE);
                        if (Scheduler.isValidTime(response))
                        {
                            plan.setDays(response);
                            showMessageDialog(window, "Days of week have been set", "Success!", INFORMATION_MESSAGE);
                        }
                        break;
                    }
                }
                plan.initiate();
            }
        });
    }

    private void setThreshold()
    {
        if (thold < 0)
            stock.setLowThreshold(thold*-1);
        else if (thold > 0 && thold < 1)
        {
            stock.setLowThreshold(1-thold);
            stock.setHighThreshold(1+thold);
        }
        else if (thold > 1)
            stock.setHighThreshold(thold);
        else if (thold == 1)
        {
            stock.setHighThreshold(1);
            stock.setLowThreshold(1);
        }
        else
        {
            stock.setHighThreshold(1.05);
            stock.setLowThreshold(0.95);
        }
    }

}
