/*
 * Copyright IBM Corp. and others 2001
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which accompanies this
 * distribution and is available at https://www.eclipse.org/legal/epl-2.0/
 * or the Apache License, Version 2.0 which accompanies this distribution and
 * is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * This Source Code may also be made available under the following
 * Secondary Licenses when the conditions for such availability set
 * forth in the Eclipse Public License, v. 2.0 are satisfied: GNU
 * General Public License, version 2 with the GNU Classpath
 * Exception [1] and GNU General Public License, version 2 with the
 * OpenJDK Assembly Exception [2].
 *
 * [1] https://www.gnu.org/software/classpath/license.html
 * [2] https://openjdk.org/legal/assembly-exception.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0 OR GPL-2.0-only WITH Classpath-exception-2.0 OR GPL-2.0-only WITH OpenJDK-assembly-exception-1.0
 */
package jit.test.jitt.exceptions;

import org.testng.annotations.Test;

@Test(groups = { "level.sanity","component.jit" })
public class SanityE3_TryCatchFinallyNested1 extends jit.test.jitt.Test {
	static class BlewIt extends Exception {
	}

	private int tstExceptionNest1(int pi) throws BlewIt {
		int mi = 323;
		try {
			try {
				mi = 645;
				if (pi == 748)
	  				throw new BlewIt();
			}
			catch (BlewIt r) {
				mi = mi + 819; 
				if (pi == 948)
					throw new BlewIt();
			}
			mi += 12100;
		}
		catch (BlewIt r) {
			mi = mi + 1036; 
			//if (pi==48) throw new BlewIt(); till jit -> interpreted works
		}
		finally {
			mi = mi + 1146;
		}
		return mi;
	}

	@Test
	public void testSanityE3_TryCatchFinallyNested1() {
		int j=4;
		
		 try  // cause resolve Special
	       {
	       	tstExceptionNest1(48);
	       }//causes JITed rtn to throw blew it
	        catch (BlewIt bcc ) 
	          {
		       j = 1;
		      };
		 try // force routine to JIT
	       {for (int i = 0; i < sJitThreshold; i++)   
		      tstExceptionNest1(52);
		   }
	        catch (BlewIt bcc ) 
	          {j = 3;
		      };
		 j = 99;
		 for (int i = 0; i < 5; i++)
		   try
	         {
	         	tstExceptionNest1(48);
		     } //throw blewIt
	          catch (BlewIt bcc ) 
	            {j = 0;
	             };			
	}

}
