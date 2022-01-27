/*
 * ESTEAM Software GmbH, 27.01.2022
 */
package org.apache.empire.commons;

public interface Unwrappable<T>
{
    /**
     * Returns true if the object is a wrapper for another object
     */
    boolean isWrapper();

    /**
     * Unwrapps am object that is wrapped by this object
     * If the object is not a wrapper then it must return itself (this)!
     * @return the original object
     */
    T unwrap();
}
