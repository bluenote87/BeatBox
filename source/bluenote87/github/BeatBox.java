/**
 * Created by Dusty on 5/23/2017.
 */
import java.awt.*;
import javax.swing.*;
import javax.sound.midi.*;
import java.util.*;
import java.awt.event.*;
import java.io.*;

public class BeatBox {

    //class scope variables
    JPanel mainPanel;
    ArrayList<JCheckBox> checkboxList; //stores the checkboxes in an arraylist
    Sequencer sequencer;
    Sequence sequence;
    Track track;
    JFrame theFrame;

    String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal", "Hand Clap", "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga", "Cowbell", "Vibraslap", "Low-mid Tom", "High Agogo", "Open Hi Conga",};
    int[] instruments = {35,42,46,38,49,39,50,60,70,72,64,56,58,47,67,63};

    public static void main(String[] args) {
        new BeatBox().buildGUI();
    }

    public void buildGUI() {
        theFrame = new JFrame("Cyber Beatbox");
        theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		try {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} 
    catch (UnsupportedLookAndFeelException e) {
       // handle exception
	   e.printStackTrace();
    }
    catch (ClassNotFoundException e) {
       // handle exception
	   e.printStackTrace();
    }
    catch (InstantiationException e) {
       // handle exception
	   e.printStackTrace();
    }
    catch (IllegalAccessException e) {
       // handle exception
	   e.printStackTrace();
    }
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        background.setBorder(BorderFactory.createEmptyBorder(10,10,10,10)); //negative space around the frame, seperates panel from border

        checkboxList = new ArrayList<JCheckBox>();
        Box buttonBox = new Box(BoxLayout.Y_AXIS);

        JButton start = new JButton("Start");
        start.addActionListener(new MyStartListener());
        buttonBox.add(start);

        JButton stop = new JButton("Stop");
        stop.addActionListener(new MyStopListener());
        buttonBox.add(stop);

        JButton upTempo = new JButton("Tempo Up");
        upTempo.addActionListener(new MyUpTempoListener());
        buttonBox.add(upTempo); //lots of GUI code

        JButton downTempo = new JButton("Tempo Down");
        downTempo.addActionListener(new MyDownTempoListener());
        buttonBox.add(downTempo);
		
		JButton saveIt = new JButton("Save This Beat");
		saveIt.addActionListener(new MySendListener());
		buttonBox.add(saveIt);
		
		JButton restoreIt = new JButton("Restore A Beat");
		restoreIt.addActionListener(new MyReadInListener());
		buttonBox.add(restoreIt);

        Box nameBox = new Box(BoxLayout.Y_AXIS);
        for (int i = 0; i < 16; i++) {
            nameBox.add(new Label(instrumentNames[i]));
        }

        background.add(BorderLayout.EAST, buttonBox);
        background.add(BorderLayout.WEST, nameBox);

        theFrame.getContentPane().add(background);

        GridLayout grid = new GridLayout(16,16);
        grid.setVgap(1);
        grid.setHgap(2);
        mainPanel = new JPanel(grid);
        background.add(BorderLayout.CENTER, mainPanel);

        for (int i = 0; i < 256; i++) {
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            checkboxList.add(c);
            mainPanel.add(c);
            //makes empty checkboxes, adds them to the ArrayList AND to the GUI panel
        }

        setUpMidi();

        theFrame.setBounds(50,50,300,300);
        theFrame.pack();
        theFrame.setVisible(true);
    } //close method

    public void setUpMidi() {
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequence = new Sequence(Sequence.PPQ,4);
            track = sequence.createTrack();
            sequencer.setTempoInBPM(120);
        } catch(Exception e) {e.printStackTrace();}
    } //close method

    //method that turns checkboxes into MIDI events and adds them to the track. this is where cool stuff happens:
    public void buildTrackAndStart() {
        int[] trackList = null;

        sequence.deleteTrack(track);
        track = sequence.createTrack(); //get rid of old track, make a new one

        for (int i = 0; i < 16; i++) { //do this for each of the ROWS, i.e. Bass, Congo, etc.
            trackList = new int[16];

            int key = instruments[i]; //set the "key" that represents which instrument this is

            for (int j = 0; j < 16; j++) { //do this for each of the BEATS in this row

                JCheckBox jc = checkboxList.get(j + 16*i);
                if (jc.isSelected()) {
                    trackList[j] = key;
                } else {
                    trackList[j] = 0;
                }
            } //close inner loop

            makeTracks(trackList); //for this instrument and for all 16 beats, make events and add them to the track
            track.add(makeEvent(176,1,127,0,16));
        } //close outer loop

        track.add(makeEvent(192,9,1,0,15)); //make sure there IS a beat at beat #16
        try {

            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY); //specify the number of loops, in this case infinitely
            sequencer.start(); //DROP
            sequencer.setTempoInBPM(120); //THAT
        } catch(Exception e) {e.printStackTrace();} //BEAT
    } //close buildTrackAndStart method

    public class MyStartListener implements ActionListener { //first inner classes, listeners for buttons
        public void actionPerformed(ActionEvent a) {
            buildTrackAndStart();
        }
    } //close inner class

    public class MyStopListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            sequencer.stop();
        }
    } //close inner class
    public class MyUpTempoListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float)(tempoFactor * 1.03));
        }
    } //close inner class
    public class MyDownTempoListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float)(tempoFactor * .97));
        }
    } //close inner class

    public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
        MidiEvent event = null;
        try {
            ShortMessage a = new ShortMessage();
            a.setMessage(comd, chan, one, two);
            event = new MidiEvent(a, tick);
        } catch (Exception e) { }
        return event;
    } //close makeEvent method

    public void makeTracks(int[] list) { //checks for note on / note off messages for one instrument at a time per beat

        for (int i = 0; i < 16; i++) {
            int key = list[i];

            if (key !=0) { //add an event if the array does not hold a zero
                track.add(makeEvent(144,9,key,100,i)); //note on and note off events
                track.add(makeEvent(128,9,key,100,i++)); //add them to the track
            }
        }
    } //close makeTracks event
	
	public class MySendListener implements ActionListener { //fires with "serializeit" button
		
		public void actionPerformed(ActionEvent a) {
			
			JFileChooser fileSave = new JFileChooser();
			fileSave.showSaveDialog(theFrame);
			saveFile(fileSave.getSelectedFile());
		} //close method
	} //close inner class
	
	public class MyReadInListener implements ActionListener { //reads previously saved tracks
		
		public void actionPerformed(ActionEvent a) {
			
			JFileChooser readSave = new JFileChooser();
			readSave.showSaveDialog(theFrame);
			readFile(readSave.getSelectedFile());
			
			sequencer.stop();
			buildTrackAndStart();
		} //close method
	} //close inner class
	
	private void saveFile(File saveTheBeat) {
		
		boolean[] checkboxState = new boolean[256]; //make a boolean array to hold the state of each "beat", i.e. check
			
			for (int i = 0; i < 256; i++) {
				JCheckBox check = (JCheckBox) checkboxList.get(i); //goes through the checkboxList array, gets its state, and adds it to the boolean array
				if (check.isSelected()) {
					checkboxState[i] = true;
				}
			}
			
			try {
				FileOutputStream fileStream = new FileOutputStream(saveTheBeat);
				ObjectOutputStream os = new ObjectOutputStream(fileStream);
				os.writeObject(checkboxState);
			} catch(Exception ex) {
				ex.printStackTrace();
			} //end serialization
	} //end method
	
	private void readFile(File restoreTheBeat) {
		
		boolean[] checkboxState = null;
			try {
				FileInputStream fileIn = new FileInputStream(restoreTheBeat);
				ObjectInputStream is = new ObjectInputStream(fileIn);
				checkboxState = (boolean[]) is.readObject(); //read the single object in the file and cast it back to a boolean array
			} catch (Exception ex) { ex.printStackTrace(); }
			
			for (int i = 0; i < 256; i++) {
				JCheckBox check = (JCheckBox) checkboxList.get(i);
				if (checkboxState[i]) {
					check.setSelected(true);
				} else {
					check.setSelected(false);
				} //restores the state of each of the checks in the checkboxList array
			}
		
	}
} //close class