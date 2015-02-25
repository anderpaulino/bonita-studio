package org.bonitasoft.studio.importer.builder;

import static org.mockito.Mockito.spy;

import org.eclipse.draw2d.geometry.Point;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class ProcBuilderTest {

    private ProcBuilder procBuilder;

    @Before
    public void setup(){
        procBuilder = spy(new ProcBuilder());
    }

    @Test
    public void shoud_set_label_on_sequence_flow() throws ProcBuilderException{
       procBuilder.setLabelPositionOnSequenceFlowOrEvent(new Point(20,30));

    }
   }
