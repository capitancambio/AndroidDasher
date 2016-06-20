/*
  This file is part of JDasher.

  JDasher is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  JDasher is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with JDasher; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

  Copyright (C) 2006      Christopher Smowton <cs448@cam.ac.uk>

  JDasher is a port derived from the Dasher project; for information on
  the project see www.dasher.org.uk; for information on JDasher itself
  and related projects see www.smowton.net/chris

*/

package dasher;

/**
 * Enumeration of possible String parameter references. See
 * CParamTables for a list and definitions.
 */
public enum Esp_parameters implements EParameters {
			SP_ORIENTATION("Orientation", "", "Orientation (TB/BT/LR/RL) - anything else = use alphabet"),
			  SP_ALPHABET_ID("AlphabetID", "", "AlphabetID"),
			  SP_ALPHABET_1("Alphabet1", "", "Alphabet History 1"),
			  SP_ALPHABET_2("Alphabet2", "", "Alphabet History 2"),
			  SP_ALPHABET_3("Alphabet3", "", "Alphabet History 3"),
			  SP_ALPHABET_4("Alphabet4", "", "Alphabet History 4"),
			  SP_COLOUR_ID("ColourID", "", "ColourID"), 
			  SP_DASHER_FONT("DasherFont", "", "DasherFont"),
			  SP_SOCKET_INPUT_X_LABEL("SocketInputXLabel", "x", "Label preceding X values for network input"),
			  SP_SOCKET_INPUT_Y_LABEL("SocketInputYLabel", "y", "Label preceding Y values for network input"),
			  SP_INPUT_FILTER("InputFilter", "Stylus Control", "Input filter used to provide the current control mode"),
			  SP_INPUT_DEVICE("InputDevice", "Mouse Input", "Driver for the input device"),
			  SP_LM_HOST("LMHost", "", "Language Model Host");
			  
			  private Esp_parameters(String rName, String def, String hr) {
					humanReadable = hr;
					defaultVal = def;
					regName = rName;
					BY_NAME.put(regName,this);
				}
				
			  private static final int SP_OFFSET = Ebp_parameters.values().length + Elp_parameters.values().length;
			  public int key() {return ordinal()+SP_OFFSET;}
			  public String regName() {return regName;}
			  public void reset(CSettingsStore ss) {ss.SetStringParameter(this, defaultVal);}
			  
			private final String regName;
			final String defaultVal;
			final String humanReadable;
}
