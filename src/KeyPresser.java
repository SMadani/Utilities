import com.sun.istack.internal.NotNull;
import static javax.swing.JOptionPane.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.util.OptionalInt;

public class KeyPresser
{
    public static void main(String[] args) throws Exception {
        new KeyPresser(true);
    }

    protected static final String EOL = System.getProperty("line.separator", "\r\n");

    protected static class KeyPress {
        final int keyCode;
        final long delay;
        final KeyType type;
        final long holdTime;

        enum KeyType {
            MOUSE_PRESS,
            KEY_PRESS
        }

        KeyPress (int keyCode, long delay, @NotNull KeyType type, long holdTime) {
            this.keyCode = keyCode;
            this.delay = delay;
            this.type = type;
            this.holdTime = holdTime;
        }

        KeyPress (int keyCode, long delay, KeyType type) {
            this (keyCode, delay, type, 70);
        }

        KeyPress (int key, long delay) {
            this (key, delay, KeyType.KEY_PRESS);
        }

        KeyPress (int key) {
            this (key, 0);
        }

        KeyPress (int key, KeyType type) {
            this (key, 0, type);
        }

        public String toStringWithoutNames() {
            //
        }

        @Override
        public String toString() {
            return "Key="+keyCode+"";
        }
    }

    protected final JFrame window;
    protected List<KeyPress> keys;

    public KeyPresser() {
        window = new JFrame("KeyPresser");
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }

    public KeyPresser (final boolean autorun) {
        this();
        if (autorun) {
            acquireKeys();
            pressKeys();
            showMessageDialog(window, "All done!", "Finished", INFORMATION_MESSAGE);
            System.exit(0);
        }
    }

    void acquireKeys() {
        keys = acquireKeys(window, 0);
    }

    void pressKeys() {
        try {
            final Object options[] = {"Start", "Quit"};
            int selected = showOptionDialog(window, "When you're ready, press \"Start\" to begin!", "Ready?", OK_CANCEL_OPTION, PLAIN_MESSAGE, null, options, options[0]);
            if (selected == OK_OPTION)
                pressKeys(keys);
            else System.exit(0);
        }
        catch (AWTException | SecurityException | NullPointerException ex) {
            showMessageDialog(window, ex.getMessage(), "Something went wrong...", ERROR_MESSAGE);
        }
    }

    protected static List<KeyPress> acquireKeys (JFrame window, int numberOfValues) {
        while (numberOfValues <= 0) try {
            String input = showInputDialog(window, "How many inputs?", 1);
            if (input == null || input.isEmpty())
                System.exit(0);
            numberOfValues = Integer.parseInt(input);
        }
        catch (NumberFormatException NaN) {
            showMessageDialog(window, "Please enter a positive integer.", "Invalid input", ERROR_MESSAGE);
        }

        List<KeyPress> keyList = new ArrayList<>(numberOfValues);
        for (int i = 1; i <= numberOfValues; i++) {
            final String mult = getQuantifier(i);
            OptionalInt keyNum = OptionalInt.empty();
            long delay = -1;
            KeyPress.KeyType type = null;

            do {
                String input = showInputDialog(window, "Please enter the name of the "+i+mult+" key.", "BUTTON1");
                if (input == null || input.isEmpty())
                    System.exit(0);
                try {
                    keyNum = OptionalInt.of(stringToKeyEvent(input));
                    type = KeyPress.KeyType.KEY_PRESS;
                }
                catch (IllegalArgumentException ignored) {
                    try {
                        keyNum = OptionalInt.of(stringToMouseEvent(input));
                        type = KeyPress.KeyType.MOUSE_PRESS;
                    } catch (IllegalArgumentException iae) {
                        showMessageDialog(window, "Please enter a valid key.", "Invalid input", ERROR_MESSAGE);
                    }
                }
            }
            while (!keyNum.isPresent());

            do {
                try {
                    String input = showInputDialog(window, "Please enter the delay (in seconds) for the "+i+mult+" key.", 1.5);
                    if (input == null || input.isEmpty())
                        System.exit(0);
                    double value = Double.parseDouble(input);
                    delay = (long) (value * 1000);
                }
                catch (NumberFormatException NaN) {
                    showMessageDialog(window, "Please enter a non-negative value.", "Invalid input", ERROR_MESSAGE);
                }
            }
            while (delay < 0);

            assert keyNum.isPresent();
            keyList.add(new KeyPress(keyNum.getAsInt(), delay, type));
        }
        return keyList;
    }

    protected static void pressKeys (@NotNull List<KeyPress> keys) throws AWTException, SecurityException {
        final Robot presser = new Robot();
        for (KeyPress key : keys) {
            try {
                Thread.sleep(key.delay);
                if (key.type.equals(KeyPress.KeyType.KEY_PRESS))
                    presser.keyPress(key.keyCode);
                else {
                    int mask = InputEvent.getMaskForButton(key.keyCode);
                    presser.mousePress(mask);
                    presser.delay(75);
                    presser.mouseRelease(mask);
                }
            }
            catch (InterruptedException | IllegalArgumentException ie) {
                ie.printStackTrace();
            }
        }
    }

    @NotNull
    protected static String getQuantifier (final int number) {
        final String numAsString = String.valueOf(number);
        final char lastInt = numAsString.charAt(numAsString.length()-1);
        switch (lastInt) {
            case '1':
                return "st";
            case '2':
                return "nd";
            case '3':
                return "rd";
            default:
                return "th";
        }
    }

    protected static ArrayList<KeyPress> loadConfig (@NotNull final File conf) {
        ArrayList<KeyPress> configList = new ArrayList<>();
        //TODO
        return configList;
    }

    protected static void saveConfig (final List<KeyPress> buttons, final File file) {
        String raw = "Key,Delay,Hold"+EOL;
        //
    }

    protected static int stringToMouseEvent (@NotNull final String input) {
        try {
            return MouseEvent.class.getField(input.toUpperCase().trim().replace(' ', '_')).getInt(null);
        }
        catch (NullPointerException | NoSuchFieldException | IllegalAccessException nex) {
            throw new IllegalArgumentException(nex.getMessage());
        }
    }

    protected static int stringToKeyEvent (@NotNull final String input) throws IllegalArgumentException {
        try {
            return KeyEvent.class.getField("VK_"+input.toUpperCase().trim().replace(' ', '_')).getInt(null);
        }
        catch (NullPointerException | NoSuchFieldException | IllegalAccessException nex) {
            throw new IllegalArgumentException(nex.getMessage());
        }
    }
}
