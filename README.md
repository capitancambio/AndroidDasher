# AndroidDasher
Dasher text entry method for Android (IME)

-----Drawing Dynamic boxes----------------------------------------------

Look for:
\app\src\main\java\dasher\CDasherViewSquare.java
RecursiveRender()
E.g. look for this lines:


    if (Render.visible())

				Screen().DrawRectangle(left, top, right, bottom, Render.m_iColour, -1, bOutline ? 1 : 0);
				.....
				
So one can similarly draw there own box. But the issue is that this boxes are dynamic -- Do someone know how this boxes can be made static?
-------------------------------------------------------
