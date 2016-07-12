package com.neurotechx.dasher;

import dasher.CButtonMode;
import dasher.CDasherComponent;
import dasher.CDasherInterfaceBase;
import dasher.Elp_parameters;

import static dasher.CDasherModel.MAX_Y;

/**
 * Created by javi on 08/07/16.
 */
public class SSVEPMode extends CButtonMode {
    public SSVEPMode(CDasherComponent creator, CDasherInterfaceBase iface, String szName) {
        super(creator, iface, szName);
    }

    protected SBox[] SetupBoxes() {
        final int iDasherY = (int)MAX_Y;

        int iForwardBoxes = 2;//ignore the conf for now(int)GetLongParameter(Elp_parameters.LP_B);
        SBox[] m_pBoxes = new SBox[iForwardBoxes+3]; // back and flashing rectangles

        // Calculate the sizes of non-uniform boxes using standard
        // geometric progression results

        // FIXME - implement this using DJCM's integer method?
        // See ~mackay/dasher/buttons/
        final double dRatio = Math.pow(129/127.0, GetLongParameter(Elp_parameters.LP_R));
        final int lpS = (int)GetLongParameter(Elp_parameters.LP_S);

        if(iForwardBoxes == 2) { // Special case for two forwards buttons
            double dNorm = 1+dRatio;

            m_pBoxes[0]=new SBox(0,(int)(iDasherY/dNorm),lpS);
            //ssvep
            m_pBoxes[1]=new SBox(0,(int)(iDasherY/dNorm*.25),lpS);

            m_pBoxes[1]=new SBox((int)(iDasherY/dNorm),iDasherY,lpS);
            m_pBoxes[2]=new SBox((int)(iDasherY/dNorm),iDasherY,lpS);
        } else {
            boolean bEven = iForwardBoxes % 2 == 0;

            int iGeometricTerms = (iForwardBoxes+1)/2;

            double dMaxSize;

            if(dRatio == 1.0) {
                dMaxSize = iDasherY / iForwardBoxes;
            } else {
                if(bEven)
                    dMaxSize = iDasherY * (dRatio - 1) / (2 * (Math.pow(dRatio, iGeometricTerms) - 1));
                else
                    dMaxSize = iDasherY * (dRatio - 1) / (2 * (Math.pow(dRatio, iGeometricTerms) - 1) - (dRatio - 1));
            }

            double dMin;

            if(bEven)
                dMin = iDasherY / 2;
            else
                dMin = (iDasherY - dMaxSize)/2;

            final int iUpBase = iForwardBoxes / 2; //round down if !bEven
            final int iDownBase = (iForwardBoxes-1)/2; //round down if bEven

            for(int i = 0; i < iGeometricTerms; ++i) { // One button reserved for backoff
                double dMax = dMin + dMaxSize * Math.pow(dRatio, i);

                m_pBoxes[iUpBase + i]=new SBox((int)dMin,(int)dMax,lpS);

                m_pBoxes[iDownBase - i]=new SBox((int)(iDasherY - dMax),(int)(iDasherY - dMin), lpS);

                dMin = dMax;
            }
        }

        m_pBoxes[m_pBoxes.length-1]=new SBox((iDasherY * (1-iForwardBoxes))/ 2,(iDasherY * (1+iForwardBoxes))/2,0,iDasherY);

        return m_pBoxes;
    }
}
