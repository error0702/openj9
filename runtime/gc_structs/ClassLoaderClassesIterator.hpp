
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

/**
 * @file
 * @ingroup GC_Structs
 */

#if !defined(HASHCLASSTABLEITERATOR_HPP_)
#define HASHCLASSTABLEITERATOR_HPP_

#include "j9.h"
#include "j9protos.h"
#include "modron.h"

#include "VMClassSlotIterator.hpp"
#include "ClassLoaderSegmentIterator.hpp"
#include "ClassHeapIterator.hpp"

class MM_GCExtensionsBase;

/**
 * Iterate over all the defined and referenced classes in a ClassLoader.
 * Note that this also includes array classes of defined classes as well
 * as system classes (Byte.TYPE, [C, ...) for the system class loader.
 * Replaced (HCR) classes are not included.
 * 
 * @ingroup GC_Structs
 */
class GC_ClassLoaderClassesIterator
{
private:
	J9JavaVM* _javaVM; /**< the JavaVM */
	J9ClassLoader *_classLoader; /**< the loader being iterated */
	J9Class *_nextClass; /**< the next class to be returned */
	J9HashTableState _walkState; /**< an opaque state structure used by VM helpers */
	GC_ClassLoaderSegmentIterator _vmSegmentIterator; /**< used for finding anonymous classes for the anonymous loader */
	GC_VMClassSlotIterator _vmClassSlotIterator; /**< used for finding system classes for the system loader */
	enum ScanModes {
		TABLE_CLASSES,
		SYSTEM_CLASSES,
		ANONYMOUS_CLASSES
	};
	ScanModes _mode; /**< indicate type of classes to be iterated */
	J9Class *_iterateArrayClazz; /**< current array walk class */
	J9Class *_startingClass; /**< the most recent table or system class which is the starting point for each array walk */
	enum ArrayClassState {
		STATE_VALUETYPEARRAY = 0, /**< setup value type array list walk */
		STATE_VALUETYPEARRAYLIST, /**< value type array list walk */
		STATE_ARRAY, /**< setup array list walk */
		STATE_ARRAYLIST, /**< array list walk */
		STATE_DONE
	};
	ArrayClassState _arrayState; /**< array walk state */
	
protected:
public:
	
private:
	/**
	 * Find the next class in the JavaVM. This should only be called in SYSTEM_CLASSES mode.
	 * @return the next system class, or NULL if finished
	 */
	J9Class *nextSystemClass();
	
	/**
	 * Find the first class in the loader's table or first Anonymous class from segment.
	 * If none, and if this is the system class loader, switch to SYSTEM_CLASSES mode and
	 * return the first system class.
	 * @return the first table class, first system class, or NULL if no classes
	 */
	J9Class *firstClass();
	
	/**
	 * Find the next anonymous class from segment
	 * @return the next anonymous class or NULL if no classes
	 */
	J9Class *nextAnonymousClass();

	/**
	 * Find the next class in the loader's table. If none, and if this is the system class 
	 * loader, switch to SYSTEM_CLASSES mode and return the first system class
	 * @return the next table class, first system class, or NULL if no classes
	 */
	J9Class *nextTableClass();
	
	/**
	 * If this is the system class loader, switch from TABLE_CLASSES to SYSTEM_CLASSES mode.
	 * @return true if this is the system class loader, false otherwise 
	 */
	bool switchToSystemMode();

	/**
	 * Iterate through all array classes of _nextClass. A base class may have
	 * a list of array classes starting at the J9Class fields nullRestrictedArrayClass
	 * and arrayClass. nullRestrictedArrayClass can only exist as a field of a value class
	 * and can only be an array of arity 1.
	 * After that its list will continue in the arrayClass field.
	 * @return the next array class, or NULL if finished
	 */
	J9Class *nextArrayClass();
	
	/**
	 * Initialize array class walk state machine.
	 */
	MMINLINE void
	initArrayClassWalk()
	{
		_startingClass = _nextClass;
		_arrayState = STATE_VALUETYPEARRAY;
		_iterateArrayClazz = NULL;
	}

protected:
	
public:
	/**
	 * Construct a new iterator for iterating over defined and referenced classes in the
	 * specified ClassLoader
	 * @param extensions[in] the GC extensions
	 * @param classLoader[in] the loader to iterate
	 */
	GC_ClassLoaderClassesIterator(MM_GCExtensionsBase *extensions, J9ClassLoader *classLoader);

	/**
	 * Fetch the next defined or referenced class.
	 * @return the next class, or NULL if finished
	 */
	J9Class *nextClass();
};

#endif /* HASHCLASSTABLEITERATOR_HPP_ */


