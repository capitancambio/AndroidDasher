package dasher;

import static dasher.CDasherModel.MAX_Y;

public class CCompassMode extends CDasherButtons {
	private int iTargetWidth;
	public CCompassMode(CDasherComponent creator, CDasherInterfaceBase iface) {
		super(creator, iface, "Compass Mode");
	}
  
	@Override protected SBox[] SetupBoxes() {
		final int iDasherY = (int)MAX_Y;

		SBox[] m_pBoxes = new SBox[4];

		iTargetWidth = (int)(iDasherY * 1024 / GetLongParameter(Elp_parameters.LP_RIGHTZOOM));

		// FIXME - need to relate these to cross-hair position as stored in the parameters

		// Not sure whether this is at all the right algorithm here - need to check
		int iTop = (2048 - iTargetWidth / 2);
		m_pBoxes[1] = new SBox(iTop, 4096 - iTop, 0);

		// Make this the inverse of the right zoom option

		int backTop = -2048 *  iTop / (2048 -  iTop);
		m_pBoxes[3] = new SBox(backTop, 4096 - backTop, 0);
  
		m_pBoxes[0] = new SBox(-iTargetWidth, iDasherY - iTargetWidth, 0);
		m_pBoxes[2] = new SBox(iTargetWidth,iDasherY + iTargetWidth, 0);

		return m_pBoxes;
	}

	@Override public boolean DecorateView(CDasherView pView, CDasherInput pInput) {
		boolean bFirst=true;
		for (int iPos = 2048 - iTargetWidth / 2; iPos >= 0; iPos -= iTargetWidth) {
			
			pView.Dasherline(-100, iPos, -1000, iPos, 1, bFirst ? 1 : 2);
			
			pView.Dasherline(-100, 4096 - iPos, -1000, 4096-iPos, 1, bFirst ? 1 : 2);
			
			bFirst = false;
		}
		return false; //never changes!
	}
 
	@Override public void HandleEvent(EParameters eParam) {
		if (eParam == Elp_parameters.LP_RIGHTZOOM) {
			m_pBoxes=SetupBoxes(); m_bDecorationChanged = true;
		}
	}
}
