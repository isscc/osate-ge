/*******************************************************************************
 * Copyright (C) 2016 University of Alabama in Huntsville (UAH)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * The US Government has unlimited rights in this work in accordance with W31P4Q-10-D-0092 DO 0105.
 *******************************************************************************/
package org.osate.ge.di;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * This annotation must not be applied to more than one method per class.
 * </p>
 * <h1>Usages</h1>
 * <table summary="Annotation Usages">
 *   <tr><th>Usage</th><th>Description</th><th>Return Value</th></tr>
 *  <tr><td>Command</td><td>Activates the command.</td><td>boolean indicating whether changes were made to diagram element or business objects.</td></tr>
 *   <tr><td>Style Factory</td><td>Internal Use Only. Returns a new style.</td><td>Style</td></tr>
 *   <tr><td>Tool</td><td>Not API. Activates the tool.</td><td>None</td></tr>
 *   <tr><td>Tooltip Contributor</td><td>Activates the tooltip contributor.</td><td>boolean</td></tr>
 * </table>
 * <h1>Named Parameters</h1>
 * <table summary="Named Parameters">
 *   <tr><th>Parameter</th><th>Usage</th><th>Description</th></tr>
 *   <tr><td>{@link org.osate.ge.di.Names#BUSINESS_OBJECT}</td><td>Tooltip Contributor</td><td>The business object for which to build the tooltip.</td></tr>
 *   <tr><td>{@link org.osate.ge.di.Names#BUSINESS_OBJECT}</td><td>Command</td><td>The business object for the currently selected diagram element. Only specified when a single diagram element is selected. If a modifying command, this object will be modifiable.</td></tr>
 *   <tr><td>{@link org.osate.ge.di.Names#BUSINESS_OBJECTS}</td><td>Command</td><td>Array containing the business objects for the currently selected diagram elements.</td></tr>
 *   <tr><td>{@link org.osate.ge.di.Names#MODIFY_BO}</td><td>Command</td><td>Object being modified as provided by GetBusinessObjectToModify. This object is modifiable.</td></tr>
 * </table>
 * <h1>Special Unnamed Parameters</h1>
 * <table summary="Unnamed Parameters">
 *   <tr><th>Type</th><th>Usage</th><th>Description</th></tr>
 *   <tr><td>Composite</td><td>Tooltip Contributor</td><td>The parent composite to which the tooltip should be added.</td></tr>
 * </table>
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Activate {
}
