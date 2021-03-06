/**
 * 
 */
package eu.europeana.shapes.testsuite;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.util.FileUtils;
import org.apache.jena.vocabulary.RDF;
import org.topbraid.shacl.vocabulary.SH;

import eu.europeana.edm.shapes.validation.RecordValidator;

/**
 * @author Hugo Manguinhas <hugo.manguinhas@europeana.eu>
 * @since 29 Mar 2016
 */
public class TestCase
{
    private String  _id      = null;
    private File    _data    = null;
    private File    _fResult = null;
    private Model   _mResult = null;

    public TestCase(String id, File data, File result)
    {
        _id      = id;
        _data    = data;
        _fResult = result;
    }

    public String getID()           { return _id; }

    /***************************************************************************
     * Public Methods - Source File
     **************************************************************************/

    public File   getDataFile()     { return _data;           }
    public String getDataAsString() { return toString(_data); }


    /***************************************************************************
     * Public Methods - Results File
     **************************************************************************/

    public File   getResultFile()     { return _fResult;           }
    public String getResultAsString() { return toString(_fResult); }

    public Integer getResultSize()
    {
        Model model = getResultModel(false);
        if ( model == null ) { return null; }

        int i = 0;
        ResIterator iter = model.listResourcesWithProperty(RDF.type
                                                         , SH.ValidationResult);
        try {
            while( iter.hasNext() ) { i++; iter.next(); }
        }
        finally { iter.close(); }
        return i;
    }

    public Model getResultModel(boolean refresh)
    {
        if ( !_fResult.exists()          ) { return null;     }
        if ( _mResult != null && !refresh) { return _mResult; }

        _mResult = ModelFactory.createDefaultModel();
        try { _mResult.read(_fResult.getAbsolutePath()); }
        catch (Throwable t)
        {
            logError("Error reading to file: " + _fResult.getAbsolutePath(), t);
        }

        return _mResult;
    }


    /***************************************************************************
     * Private Methods
     **************************************************************************/

    private void saveResult(TestResult result)
    {
        Lang lang = RDFLanguages.filenameToLang(_fResult.getName());
        if ( lang == null ) { return; }

        _fResult.getParentFile().mkdirs();
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(_fResult);
            result.getOutput().write(fos, lang.getLabel());
            fos.flush();
        }
        catch (IOException e)
        {
            logError("Error writing to file: " + _fResult.getAbsolutePath(), e);
        }
        finally { IOUtils.closeQuietly(fos); }
    }

    public TestResult run(RecordValidator v)
    {
        try
        {
            TestResult tr = new TestResult(this, v.validate(_data));
            if ( !_fResult.exists() ) { saveResult(tr); }
            return tr;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private void logError(String msg, Throwable t)
    {
        System.err.println(msg + ", reason: " + t.getMessage());
    }

    private String toString(File file)
    {
        FileInputStream fis = null;
        try { 
            fis = new FileInputStream(file);
            return FileUtils.readWholeFileAsUTF8(fis);
        }
        catch (IOException e) {
            logError("Error reading file: " + file.getName(), e);
            return "";
        }
        finally { IOUtils.closeQuietly(fis); }
    }
}
