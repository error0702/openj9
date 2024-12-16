/*******************************************************************************
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
 *******************************************************************************/

/**
 * @file
 * @ingroup Shared_Common
 */

#if !defined(ROMCLASSMANAGER_HPP_INCLUDED)
#define ROMCLASSMANAGER_HPP_INCLUDED

/* @ddr_namespace: default */
#include "Manager.hpp"
#include "ClasspathItem.hpp"

struct LocateROMClassResult {
	ROMClassWrapper* known;
	const ShcItem* knownItem;
	IDATA foundAtIndex;
	ClasspathEntryItem* staleCPEI;
};

/* result flags */
#define LOCATE_ROMCLASS_RETURN_FOUND	 1
#define LOCATE_ROMCLASS_RETURN_NOTFOUND 0

/* action flags */
#define LOCATE_ROMCLASS_RETURN_DO_MARK_CPEI_STALE 2
#define LOCATE_ROMCLASS_RETURN_DO_TRY_WAIT 4

/* information flags */
#define LOCATE_ROMCLASS_RETURN_MARKED_ITEM_STALE 8
#define LOCATE_ROMCLASS_RETURN_FOUND_SHADOW 16

/**
 * Sub-interface of SH_Manager used for managing ROMClasses in the cache
 *
 * @see SH_ROMClassManagerImpl.hpp
 * @ingroup Shared_Common
 */
class SH_ROMClassManager : public SH_Manager
{
public:
	typedef char* BlockPtr;

	virtual UDATA locateROMClass(J9VMThread* currentThread, const char* path, U_16 pathLen, ClasspathItem* cp, I_16 cpeIndex, IDATA confirmedEntries, IDATA callerHelperID, 
					const J9ROMClass* cachedROMClass, const J9UTF8* partition, const J9UTF8* modContext, LocateROMClassResult* result) = 0;

	virtual const J9ROMClass* findNextExisting(J9VMThread* currentThread, void * &findNextIterator, void * &firstFound, U_16 classnameLength, const char* classnameData) = 0;

	virtual UDATA existsClassForName(J9VMThread* currentThread, const char* path, UDATA pathLen) = 0;
	
};

#endif /* !defined(ROMCLASSMANAGER_HPP_INCLUDED) */


