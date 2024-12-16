
/*******************************************************************************
 * Copyright IBM Corp. and others 1991
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
 *******************************************************************************/
 
#if !defined(EVENT_GC_START_HPP_)
#define EVENT_GC_START_HPP_
 
#include "j9.h"
#include "j9cfg.h"
#include "mmhook.h"

#include "VerboseEvent.hpp"

/**
 * Stores the data relating to the start of a GC.
 * @ingroup GC_verbose_events
 */
class MM_VerboseEventGCStart : public MM_VerboseEvent
{
protected:
	/**
	 * Passed Data
	 * @{ 
	 */
	MM_CommonGCStartData _gcStartData; /**< data which is common to all GC start events */
	/** @} */
 	
	/**
	 * External Data
	 * @{ 
	 */
	I_64 _timeInMilliSeconds;
	/** @} */
	
	void initialize(void);
	
	bool hasDetailedTenuredOutput();
	void tlhFormattedOutput(MM_VerboseOutputAgent *agent);
	void loaFormattedOutput(MM_VerboseOutputAgent *agent);

public:

	void gcStartFormattedOutput(MM_VerboseOutputAgent *agent);

	MMINLINE virtual bool definesOutputRoutine() { return true; };
	MMINLINE virtual bool endsEventChain() { return false; };

	MM_VerboseEventGCStart(OMR_VMThread *omrVMThread, U_64 timestamp, UDATA type, MM_CommonGCStartData* gcStartData, J9HookInterface** hookInterface) :
		MM_VerboseEvent(omrVMThread, timestamp, type, hookInterface),
		_gcStartData(*gcStartData)
	{}
};

#endif /* EVENT_GC_START_HPP_ */
