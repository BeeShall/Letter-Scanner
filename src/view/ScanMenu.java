package view;

import java.util.Collection;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.robotics.navigation.DifferentialPilot;
import lejos.utility.Delay;
import lejos.utility.TextMenu;
import model.Dictionary;

/************************************************************
* Name:  Bishal Regmi                                      *
* Project:  Project 4 - Scanner                            *
* Class:  CMPS 331 - Artificial Intelligence               *
* Date:  4/9/2016                                          *
************************************************************/

/**
 * @author Bishal
 * Class that handles the Scan Menu and searches. This is a child of the Menu Interface.
 */
public class ScanMenu extends Menu {
	private Robot robot;
	private BoardAttributes boardAttr;
	private Dictionary dictionary;
	private DifferentialPilot pilot;

	/**
	 * Description : Constructor for the Scan Menu.
	 * @param previous: Menu to return to after finishing
	 * @param robot: The robot object that we have been working on
	 * @param boardAttr: the attributes of the we are working on
	 * @param dictionary: the dictionary that holds the rules and searches for this session.
	 */
	public ScanMenu(Menu previous, Robot robot, BoardAttributes boardAttr, Dictionary dictionary) {
		super(previous);
		this.boardAttr = boardAttr;
		this.robot = robot;
		this.lcd = robot.getLCD();
		this.dictionary = dictionary;

		pilot = robot.getPilot();
		// TODO Auto-generated constructor stub
	}

	/** 
	 * Description: Inherited method to invoke the class. Once this method is called, the menu is drawn on the screen and action is taken based on the user's menu selection.
	 * @return Menu - returns the Menu to go after depending on user's selection 
	 */
	@Override
	public Menu invokeMenu() {
		// TODO Auto-generated method stub
		drawMenu();
		int xCoord = 0;
		int yCoord = 2;
		displayCursor(xCoord, yCoord, true);
		while (true) {
			int buttonId = Button.waitForAnyPress();
			displayCursor(xCoord, yCoord, false);
			if (buttonId == Button.ID_UP || buttonId == Button.ID_DOWN) {
				if (yCoord == 2)
					yCoord++;
				else if (yCoord == 3)
					yCoord--;
			} else if (buttonId == Button.ID_ENTER) {
				lcd.clear();
				if (yCoord == 2)
					conductForwardSearch();
				else if (yCoord == 3)
					conductBackwardSearch();
			} else if (buttonId == Button.ID_ESCAPE) {
				return previous;
			}
			displayCursor(xCoord, yCoord, true);
		}
	}
	
	/** 
	 * Description: Method to write the strings on the screen 
	 */
	private void drawMenu() {
		lcd.clear();
		lcd.drawString("Scan Menu", 0, 0);
		lcd.drawString("-----------------", 0, 1);
		lcd.drawString(" 1. Forward Search", 0, 2);
		lcd.drawString(" 2. Backward Search", 0, 3);
	}

	/** 
	 * Description: Method to conduct the forward search 
	 */
	private void conductForwardSearch() {
		// TODO Auto-generated method stub
		Delay.msDelay(3000);
		//gets the margin to differentiate between on and off color
		float margin = boardAttr.getMargin();
		double pixelSize = boardAttr.getPixelSize();
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 6; j++) {
				boolean isPixelSet = false;
				pilot.travel(pixelSize);
				float colorRead = robot.getFloorColorValue();
				Sound.beep();
				lcd.drawString("Row: " + i, 0, 0);
				lcd.drawString("Column: " + j, 0, 1);
				Delay.msDelay(1000);
				//if the color read is onColor
				if (colorRead >margin)
					isPixelSet = true;
				//get all the ramining candidates and print them
				String[] remainingCandidates = dictionary.frontSearch(i, j, isPixelSet);
				lcd.clear();
				lcd.drawString("Remaining:", 0, 0);
				for (int k = 0; k < remainingCandidates.length; k++) {
					lcd.drawString(remainingCandidates[k], 0, k + 1);
				}
				Delay.msDelay(2000);
				lcd.clear();
			}
			//if this is the last cell don't rotate the robot
			if(i!=7){
				rotateRobot(pixelSize);
			}
		}
		int printIndex = 2;
		//if any full pattern is found
		if (dictionary.isFound()) {
			Sound.beepSequenceUp();
			lcd.drawString("Success!", 0, 0);
			lcd.drawString("Matched:", 0, 1);
			lcd.drawString("Full Pattern: " + dictionary.getFullPattern(), 0, 2);
			printIndex = 3;
		} else {
			//if no full pattern found
			Sound.twoBeeps();
			lcd.drawString("Failure!", 0, 0);
			lcd.drawString("Possible:", 0, 1);
		}
		//print all the deduced candidates
		String[] remainingCandidates = dictionary.getRecognizedCandidatesFromForwardSearch();
		for (int k = 0; k < remainingCandidates.length; k++) {
			lcd.drawString(remainingCandidates[k], 0, k + printIndex);
		}
		Button.waitForAnyPress();
		lcd.clear();
		drawMenu();
		dictionary.resetForwardSearch();
		return;
	}

	/** 
	 * Description: Method to conduct the forward search 
	 */
	private void conductBackwardSearch() {
		// TODO Auto-generated method stub
		String selectedString = displayStringSelector();
		if (selectedString == null){
			lcd.clear();
			drawMenu();
			return;
		}
		try {
			//setting the letter to search for using backward search
			dictionary.setBackwardSearchLetter(selectedString);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// backwardSearch Algorithm
		Delay.msDelay(3000);
		double pixelSize = boardAttr.getPixelSize();
		float margin = boardAttr.getMargin();
		for (int i = 0; i < boardAttr.getRow(); i++) {
			for (int j = 0; j < boardAttr.getCol(); j++) {
				lcd.clear();
				pilot.travel(pixelSize);
				//if the required pixel is supposed to be on, only then read the color and check it is on or off
				if (dictionary.backWardSearch(i, j)) {
					float colorRead = robot.getFloorColorValue();
					Sound.beep();
					lcd.drawString("Row: " + i, 0, 0);
					lcd.drawString("Column: " + j, 0, 1);
					Delay.msDelay(1000);
					//if the color is off abort
					if (colorRead < margin) {
						lcd.clear();
						Sound.twoBeeps();
						lcd.drawString("Failure!", 0, 0);
						Button.waitForAnyPress();
						lcd.clear();
						drawMenu();
						dictionary.resetBackwardSearch();
						return;
					}
				}
			}
			if(i!=7){
				rotateRobot(pixelSize);
			}
		}
		lcd.clear();
		Sound.beepSequenceUp();
		lcd.drawString("Success!", 0, 0);
		Button.waitForAnyPress();
		lcd.clear();
		drawMenu();
	}

	/** 
	 * Description: Method to rotate the robot after scanning each row.
	 * @param travelDistance : the pixelSize i.e. the distance robot needs to travel every single time
	 */
	private void rotateRobot(double travelDistance) {
		//bring back the robot using the distance it has traveled
		pilot.travel(-6 * travelDistance);
		//rotate right
		pilot.rotate(-90);
		//travel 1 pixel
		pilot.travel(travelDistance);
		//rotate left
		pilot.rotate(90);
	}

	/**
	 * Description: Method to display the selection menu to select character for backward search
	 * @return the selected string by the user
	 */
	private String displayStringSelector() {
		//fetch all the strings in the rule base
		Collection<String> strings = dictionary.getStringsinRules();
		String[] stringsy = strings.toArray(new String[strings.size()]);

		int index = -1;
		
		
		Delay.msDelay(300);		
		while (!Button.ENTER.isDown()) {
			TextMenu menu = new TextMenu(stringsy, 1, "Choose a character");
			index = menu.select();
			if(Button.ESCAPE.isDown()){
				return null;
			}
			
		}
		//if the user selected something, return the string at that index
		return stringsy[index];		
	}	
}
