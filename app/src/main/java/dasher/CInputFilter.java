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
 * Input filters are a key part of the Dasher infrastructure.
 * <p>
 * They are responsible for taking raw input co-ordinates and
 * modifying the DasherModel in some way if appropriate.
 * <p>
 * They are also notified of all key presses and are given a chance
 * to apply graphical decorations towards the end of the drawing
 * cycle.
 * <p>
 * The most important method is Timer, which will be fired at the
 * beginning of each new frame. This is responsible for obtaining
 * input co-ordinates from the View, typically using the GetCoordinates
 * method, and interacting with the model in some way, typically
 * by calling Tap_on_display.
 * <p>
 * Timer will always be called every frame, regardless of the filter
 * in use, except for when Dasher is paused or locked.
 * <p>
 * Being Modules, InputFilters have an Activate and Deactivate
 * method which is intended to allow the allocation and deallocation
 * of temporary helper objects, helper threads, network connections
 * and so forth.
 * <p>
 * It is very important that the Timer method should return as near
 * instantaneously as possible, as this will be called tens of times
 * per second.
 * <p>
 * Input filters may also interact with the main Dasher interface,
 * typically to start and stop Dasher in response to key presses.
 */
public abstract class CInputFilter extends CDasherComponent implements CDasherModule {
	
	/**
	 * Interface which this InputFilter may control
	 */	
	protected final CDasherInterfaceBase m_Interface;
	
	private final String m_szName;
	
	/**
	 * Creates a new filter. See DasherModule for information
	 * on registering this with the Interface as a usable
	 * module.
	 * 
	 * @param EventHandler Event handler with which we should register ourselves
	 * @param SettingsStore Settings repository to use
	 * @param iID Unique identifier for this Module
	 * @param iType Module type (see DasherModule for a list)
	 * @param szName Friendly name (ideally unique)
	 * @see CDasherModule
	 */
	public CInputFilter(CDasherComponent creator, CDasherInterfaceBase iface, String szName) { 
      	super(creator);
      	this.m_szName = szName;
      	m_Interface = iface;
    }
	
	public String getName() {return m_szName;}
	
	/**
	 * Should draw any decorations applicable to this filter.
	 * <p>
	 * For example, DefaultFilter might draw a line indicating
	 * the mouse position at this stage.
	 * <p>
	 * If we wish to allow any helper classes to do their own
	 * decorating, we should pass the View to them at this stage.
	 * 
	 * @param View View to which we should draw our decorations.
	 * @return True if anything has been drawn, false otherwise.
	 */
	public boolean DecorateView(CDasherView View, CDasherInput pInput) { return false; };
	
	/**
	 * Notifies the filter of a key-down event at a given time,
	 * and allows it to make changes to the Model but not the View
	 * in response to this change. If we wish to display things
	 * in response to a key event, we must store state in the meantime
	 * and wait for the next frame. 
	 * 
	 * @param Time System time at which the key event took place, as a unix timestamp.
	 * @param iId ID of the key pressed (see DasherInterfaceBase.KeyDown for a list)
	 * @param pView View in which co-ordinates may be transformed
	 * @param pInput Current input device from which coordinates may be obtained
	 * @param pModel Model which we may manipulate in response to this event
	 */
	public void KeyDown(long Time, int iId, CDasherView pView, CDasherInput pInput, CDasherModel Model) {};

	/**
	 * Notifies the filter of a key-up event at a given time,
	 * and allows it to make changes to the Model but not the View
	 * in response to this change. If we wish to display things
	 * in response to a key event, we must store state in the meantime
	 * and wait for the next frame. 
	 * 
	 * @param Time System time at which the key event took place, as a unix timestamp.
	 * @param iId ID of the key pressed (see DasherInterfaceBase.KeyDown for a list)
	 * @param pView View in which co-ordinates may be transformed
	 * @param pInput Current input device from which coordinates may be obtained
	 * @param pModel Model which we may manipulate in response to this event
	 * 	 */
	public void KeyUp(long Time, int iId, CDasherView pView, CDasherInput pInput, CDasherModel Model) {};
	
	/**
	 * Called every frame; the filter may use this opportunity to schedule
	 * further (per-frame) movement if desired.
	 * <p>
	 * The View is passed so that input co-ordinates can be resolved
	 * to Dasher co-ordinates prior to interacting with the Model;
	 * drawing should not be done at this stage but should take place
	 *  during the call to DecorateView.
	 * <p>
	 * The default implementation does nothing, which is appropriate for 
	 * filters which operate by scheduling zooms in response to user
	 * input events, rather than in response to timer callbacks
	 * 
	 * @param Time System time as a unix timestamp
	 * @param pView View in which co-ordinates may be transformed
	 * @param pInput Current input device from which coordinates may be obtained
	 * @param pModel Model which we may manipulate in response to this event
	 */
	public void Timer(long Time, CDasherView pView, CDasherInput pInput, CDasherModel pModel) {}
	
	/**
	 * Activates this filter; now is the time to start helper threads,
	 * allocate resources, and so forth.
	 *
	 */
	public void Activate() {};
	
	/**
	 * Deactivates this filter; threads should be stopped and resources
	 * which can be freed should be.
	 *
	 */
	public void Deactivate() {}

	public boolean supportsPause() {return false;}
	
	public void pause() {}
	
}

class CStaticFilter extends CInputFilter {
	CStaticFilter(CDasherComponent creator, CDasherInterfaceBase iface, String szName) {
		super(creator, iface, szName);
	}
	
	private CDasherModel model;
	
	protected void scheduleZoom(CDasherModel model, long x, long y) {
		(this.model=model).ScheduleZoom(x, y);
		m_Interface.Redraw(false);
	}
	
	@Override public void pause() {
		if (model!=null) model.clearScheduledSteps();
	}
	
}