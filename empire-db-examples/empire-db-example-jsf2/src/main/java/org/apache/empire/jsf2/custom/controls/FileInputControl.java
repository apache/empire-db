/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.empire.jsf2.custom.controls;

import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlInputText;
import javax.faces.context.FacesContext;

import org.apache.empire.exceptions.InternalException;
import org.apache.empire.exceptions.UnexpectedReturnValueException;
import org.apache.empire.jsf2.controls.InputControl;

public class FileInputControl extends InputControl
{
    public static final String NAME = "file";

    private Class<? extends HtmlInputFile> inputComponentClass = null;

    public FileInputControl(Class<? extends HtmlInputFile> inputComponentClass)
    {
        super(NAME);
        this.inputComponentClass = inputComponentClass;
    }

    public FileInputControl()
    {
        this(HtmlInputFile.class);
    }

    @Override
    protected void createInputComponents(UIComponent parent, InputInfo ii, FacesContext context, List<UIComponent> compList)
    {
        HtmlInputFile input;
        try
        {
            input = inputComponentClass.newInstance();
            copyAttributes(parent, ii, input);
        }
        catch (InstantiationException e1)
        {
            throw new InternalException(e1);
        }
        catch (IllegalAccessException e2)
        {
            throw new InternalException(e2);
        }
        compList.add(input);
        // update
        updateInputState(compList, ii, context, true);
    }

    @Override
    protected void updateInputState(List<UIComponent> compList, InputInfo ii, FacesContext context, boolean setValue)
    {
        UIComponent comp = compList.get(0);
        if (!(comp instanceof HtmlInputFile))
        {
            throw new UnexpectedReturnValueException(comp.getClass().getName(), "compList.get(0)");
        }
        // update state
        HtmlInputFile input = (HtmlInputFile) comp;
        input.setDisabled(ii.isDisabled());
        // set value
        if (setValue)
            setInputValue(input, ii);
    }

    public class HtmlInputFile extends HtmlInputText
    {
        public static final String RENDER_TYPE = "org.apache.empire.jsf2.custom.controls.FileInputRenderer";

        @Override
        public String getRendererType()
        {
            return RENDER_TYPE;
        }
    }

    /*
    public class FileInputRenderer extends TextRenderer
    {

        // Constants ----------------------------------------------------------------------------------

        private static final String EMPTY_STRING     = "";
        private final Attribute[]   INPUT_ATTRIBUTES = AttributeManager.getAttributes(AttributeManager.Key.INPUTTEXT);

        // Actions ------------------------------------------------------------------------------------

        @Override
        protected void getEndTextToRender(FacesContext context, UIComponent component, String currentValue)
            throws IOException
        {
            ResponseWriter writer = context.getResponseWriter();
            writer.startElement("input", component);
            writeIdAttributeIfNecessary(context, writer, component);
            writer.writeAttribute("type", "file", null);
            writer.writeAttribute("name", (component.getClientId(context)), "clientId");

            // Render styleClass, if any.
            String styleClass = (String) component.getAttributes().get("styleClass");
            if (styleClass != null)
            {
                writer.writeAttribute("class", styleClass, "styleClass");
            }

            // Render standard HTMLattributes expect of styleClass.
            RenderKitUtils.renderPassThruAttributes(context, writer, component, INPUT_ATTRIBUTES, getNonOnChangeBehaviors(component));
            RenderKitUtils.renderXHTMLStyleBooleanAttributes(writer, component);
            RenderKitUtils.renderOnchange(context, component, false);

            writer.endElement("input");
        }

        @Override
        public void decode(FacesContext context, UIComponent component)
        {
            rendererParamsNotNull(context, component);
            if (!shouldDecode(component))
            {
                return;
            }
            String clientId = decodeBehaviors(context, component);
            if (clientId == null)
            {
                clientId = component.getClientId(context);
            }

            //            File file = ((MultipartRequest) context.getExternalContext().getRequest()).getFile(clientId);
            // If no file is specified, set empty String to trigger validators.
            //            ((UIInput) component).setSubmittedValue((file != null) ? file : EMPTY_STRING);
        }

        @Override
        public Object getConvertedValue(FacesContext context, UIComponent component, Object submittedValue)
            throws ConverterException
        {
            return (submittedValue != EMPTY_STRING) ? submittedValue : null;
        }

    }
    */

}
