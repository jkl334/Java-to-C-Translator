package nyu.segfault;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import xtc.lang.*;
import xtc.parser.*;
import xtc.tree.*;
import xtc.util.*;

/**
 * Unit test for simple App.
 */
public class SampleTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public SampleTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( SampleTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        assertTrue( true );
    }
}
