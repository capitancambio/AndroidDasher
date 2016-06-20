package dasher;

import static dasher.CDasherModel.CROSS_Y;

public class OneButtonDynamicFilter extends CDynamicButtons {

	private BounceMarker upInner, downInner, upOuter, downOuter;
	
	private boolean m_bDecorationChanged;
	private double m_dNatsAtLastApply;
	private int m_iKeyHeldId=-1;
	/** Time at which button was first pressed (/pressed down, if we're using the release time),
	 * or Long.MAX_VALUE if no such first press has occurred.
	 */
	private long m_iFirstPressTime = Long.MAX_VALUE;
	private double m_dNatsAtFirstPress;
	
	/** locations of up/down guidelines (DasherY) - computed in Timer */
	private int guideUp, guideDown;
	private double m_dMulBoundary;
	private boolean m_bUseUpGuide;
	
	public OneButtonDynamicFilter(CDasherComponent creator, CDasherInterfaceBase iface) {
		super(creator, iface, "One-button Dynamic Mode");
		createMarkers();
	}
	
	@Override
	public boolean DecorateView(CDasherView pView, CDasherInput pInput) {
		int y= (int)(downInner.m_iLocn*m_dMulBoundary);
		pView.Dasherline(-100, 2048-y, -1000, 2048-y, 1, 62);
		y=(int)(upInner.m_iLocn * m_dMulBoundary);
		pView.Dasherline(-100, 2048-y, -1000, 2048-y, 1, 62);
		
		//Moving markers...
		if (m_iFirstPressTime!=Long.MAX_VALUE) {
			int upCol=m_bUseUpGuide ? 240 : 61, downCol=240+61-upCol; //240 = green = active, 61 = orange/yellow = inactive
			pView.Dasherline(-100, 2048-guideUp, -1000, 2048-guideUp, 3, upCol);
			pView.Dasherline(-100, 2048-guideDown, -1000, 2048-guideDown, 3, downCol);
		}
		//Fixed markers - draw last so they go on top...
		upInner.Draw(pView);
		upOuter.Draw(pView);
		downInner.Draw(pView);
		downOuter.Draw(pView);
		
		if (m_bDecorationChanged) {
			m_bDecorationChanged=false;
			return true;
		}
		return false;
	}

	@Override protected void TimerImpl(long iTime, CDasherView pView, CDasherModel pModel) {
		if (m_iFirstPressTime!=Long.MAX_VALUE) {
			double dGrowth = Math.exp(pModel.GetNats()-m_dNatsAtFirstPress);
			guideUp = (int)(dGrowth*upInner.m_iLocn);
			guideDown = (int)(dGrowth*downInner.m_iLocn);
			m_bUseUpGuide = dGrowth < m_dMulBoundary;
			CDasherView.DRect visReg = pView.VisibleRegion();
			if (2048-guideUp < visReg.minY && 2048-guideDown>visReg.maxY) {
				//both markers outside y-axis (well, in compressed region)
				// => waited too long for second press(/release)
				reverse(iTime, pModel);
				return; //hmmm. without scheduling anything?
			}
		}
		pModel.ScheduleOneStep(0, CROSS_Y, iTime, getSpeedMul(pModel, iTime));
	}
	
	@Override
	public void HandleEvent(EParameters eParam) {
		if (eParam==Elp_parameters.LP_ONE_BUTTON_OUTER 
			|| eParam==Elp_parameters.LP_ONE_BUTTON_LONG_GAP
			|| eParam==Elp_parameters.LP_ONE_BUTTON_SHORT_GAP)
			createMarkers();
		super.HandleEvent(eParam);
	}
	
	private void createMarkers() {
		final int outer = (int)GetLongParameter(Elp_parameters.LP_ONE_BUTTON_OUTER),
				biggap = (int)GetLongParameter(Elp_parameters.LP_ONE_BUTTON_LONG_GAP),
				down = outer - biggap,
				up = outer - (int)(biggap * GetLongParameter(Elp_parameters.LP_ONE_BUTTON_SHORT_GAP))/100;
				
		upInner = new BounceMarker(up);
		upOuter = new BounceMarker(outer);
		downInner = new BounceMarker(-down);
		downOuter = new BounceMarker(-outer);
		m_dMulBoundary = outer/Math.sqrt(up*down);
	}
	
	@Override public void KeyDown(long iTime, int iId, CDasherView pView, CDasherInput pInput, CDasherModel pModel) {
		if (m_iKeyHeldId!=-1 && iId != m_iKeyHeldId) return; //ignore subsequent presses whilst button down
		m_iKeyHeldId=iId;
		if (isReversing())
			pause();
		else if (isPaused())
			run(iTime, pModel);
		else if (m_iFirstPressTime==Long.MAX_VALUE) {
			m_iFirstPressTime = iTime;
			m_dNatsAtFirstPress = pModel.GetNats();
		} else
			secondPress(iTime,pModel);
	}
	
	@Override public void KeyUp(long iTime, int iId, CDasherView pView, CDasherInput pInput, CDasherModel pModel) {
		if (iId == m_iKeyHeldId) {
			m_iKeyHeldId=-1;
			if (m_iFirstPressTime!=Long.MAX_VALUE && GetBoolParameter(Ebp_parameters.BP_ONE_BUTTON_RELEASE_TIME))
				secondPress(iTime,pModel);
		}
	}
	
	protected void secondPress(long iTime, CDasherModel pModel) {
		BounceMarker inner,outer;
		if (m_bUseUpGuide) {
			inner = upInner; outer=upOuter;
		} else {
			inner = downInner; outer = downOuter;
		}
		double dCurBitrate = GetLongParameter(Elp_parameters.LP_MAX_BITRATE) /100.0;
		int iOffset = inner.GetTargetOffset(dCurBitrate*getSpeedMul(pModel, iTime), outer, iTime - m_iFirstPressTime);
		if (pModel.m_iDisplayOffset!=0) {
			iOffset -= pModel.m_iDisplayOffset;
			System.err.println("Display Offset "+pModel.m_iDisplayOffset+" reducing to "+iOffset);
		}
		double dNewNats = pModel.GetNats() - m_dNatsAtLastApply;
		upInner.NotifyOffset(iOffset, dNewNats); upOuter.NotifyOffset(iOffset, dNewNats);
		downInner.NotifyOffset(iOffset, dNewNats); downOuter.NotifyOffset(iOffset, dNewNats);
		inner.RecordPush(iOffset, pModel.GetNats() - m_dNatsAtFirstPress, dCurBitrate);
		outer.RecordPush(iOffset, 0.0, dCurBitrate);
		ApplyOffset(pModel, iOffset);
		m_dNatsAtLastApply = pModel.GetNats();
		m_iFirstPressTime = Long.MAX_VALUE;
	}
	
	@Override public void reverse(long iTime, CDasherModel pModel) {
		upInner.clearPushes(); upOuter.clearPushes(); downInner.clearPushes(); downOuter.clearPushes();
		m_iFirstPressTime = Long.MAX_VALUE;
		super.reverse(iTime, pModel);
	}
	
}
