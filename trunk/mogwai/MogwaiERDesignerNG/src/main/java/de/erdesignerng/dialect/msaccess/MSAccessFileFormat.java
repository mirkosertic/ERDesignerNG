
package de.erdesignerng.dialect.msaccess;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public final class MSAccessFileFormat {

    private static final Logger LOGGER = Logger.getLogger(MSAccessFileFormat.class);

    public static final String JET = "Jet";

    public static final String ACE = "ACE";

    private static final int DEFAULT_ENGINE_NAME_OFFSET = 13;

    private static final int DEFAULT_HEADER_SIZE = 24;

    private final String theName;

    private final int theHeaderSize;

    private final int theFlagOffset;

    private final char theFlagValue;

    private final int theNameOffset;

    private final String theIdentifyingTable;

    public MSAccessFileFormat(String anEngineName, int aHeaderSize, int anEngineFlagOffset, int anEngineFlagValue, int anEngineNameOffset, String anIdentifyingTable) {
        if ((anEngineFlagOffset < aHeaderSize) && (anEngineNameOffset + anEngineName.length() < aHeaderSize)) {
            theName = anEngineName;
            theHeaderSize = aHeaderSize;
            theFlagOffset = anEngineFlagOffset;
            theFlagValue = (char)anEngineFlagValue;
            theNameOffset = anEngineNameOffset;
            theIdentifyingTable = anIdentifyingTable;
        } else {
            throw new RuntimeException("Inconsistent header definition.");
        }
    }

    public MSAccessFileFormat(String anEngineName, int anEngineFlagOffset, int anEngineFlagValue, int anEngineNameOffset, String anIdentifyingTable) {
        this(anEngineName, DEFAULT_HEADER_SIZE, anEngineFlagOffset, anEngineFlagValue, anEngineNameOffset, anIdentifyingTable);
    }

    public MSAccessFileFormat(String anEngineName, int anEngineFlagOffset, int anEngineFlagValue, String anIdentifyingTable) {
        this(anEngineName, anEngineFlagOffset, anEngineFlagValue, DEFAULT_ENGINE_NAME_OFFSET, anIdentifyingTable);
    }

    public MSAccessFileFormat(String anEngineName, int anEngineFlagOffset, int anEngineFlagValue) {
        this(anEngineName, anEngineFlagOffset, anEngineFlagValue, DEFAULT_ENGINE_NAME_OFFSET, null);
    }

    /**
     * Reads the version of the engine from the binary.
     *
     * Attention: Access 2000, 2002 and 2003 can *not* be devided!
     *
     * @see http://msdn.microsoft.com/en-us/library/aa139959%28office.10%29.aspx
     * @param aFileName
     * @param aComareWholeFile - searches the whole file if true
     *
     * @return true if the the file matches the defined version
     */
    public final boolean matches(String aFileName, boolean aComareWholeFile) {
        String theHeader = null;
        boolean theResult = false;

        if (aFileName != null) {
            // binary read required header bytes
            try {

                byte[] buffer = new byte[theHeaderSize];
                FileInputStream f = new FileInputStream(aFileName);
                f.read(buffer);
                theHeader = new String(buffer);

            } catch (FileNotFoundException ex) {
                LOGGER.error("Cannot find database file " + aFileName, ex);
            } catch (IOException ex) {
                LOGGER.error("Cannot read database file " + aFileName, ex);
            }

            if (!StringUtils.isEmpty(theHeader)) {
                // evaluate the engine information
                theResult = ((theHeader.charAt(theFlagOffset) == theFlagValue) && (theHeader.substring(theNameOffset, theNameOffset + theName.length()).equals(theName)));

                // additionally search binary for occurance of a special tablename
                // TODO needs tuning, avoid reading entire file
                if (aComareWholeFile && theResult && !StringUtils.isEmpty(theIdentifyingTable)) {
                    theResult = (findInFile(aFileName, expand(theIdentifyingTable, 0)) > theHeaderSize);
                }
            }
        }

        return theResult;

    }

    /**
     * Reads the version of the engine from the binary and searches for
     * special system tables throuph the connection to classifie the
     * version more detailled.
     *
     * Attention: Access 2002 and 2003 still *not* be devided!
     *
     * @see http://msdn.microsoft.com/en-us/library/aa139959%28office.10%29.aspx
     */
    public final boolean matches(Connection aConnection) {

        String theFile = null;
        boolean theResult = false;

        // get specifiled database file from the connection URL
        try {
            String[] theParamArray = aConnection.getMetaData().getURL().split(";");
            for (int i = 0; i < theParamArray.length; i++) {
                if (theParamArray[i].toUpperCase().startsWith("DBQ")) {
                    theParamArray = theParamArray[i].split("=");
                    theFile = theParamArray[1];
                    break;
                }
            }

            if (!StringUtils.isEmpty(theFile)) {
                // evaluate the engine information
                theResult = matches(theFile, false);

                // additionally search the database for a special tablename
                if (theResult && !StringUtils.isEmpty(theIdentifyingTable)) {
                    theResult = (getTableCount(aConnection, theIdentifyingTable) == 1);
                }
            }
        } catch (SQLException e) {}

        return theResult;

    }

    private static final int getTableCount(Connection aConnection, String aTableName) throws SQLException {

        final String theColumnName = "theCount";
        short theResult = 0;
        String theSQL = "SELECT Count(MSysObjects.Id) AS " + theColumnName + " " +
                        "FROM MSysObjects " +
                        "WHERE (MSysObjects.Name LIKE ?);";

        PreparedStatement theStatement = aConnection.prepareStatement(theSQL);
        theStatement.setString(1, aTableName);

        ResultSet theIdentificationResult = theStatement.executeQuery();

        if (theIdentificationResult != null) {
            if  (theIdentificationResult.next()) {
                theResult = theIdentificationResult.getShort(theColumnName);
            }

            theIdentificationResult.close();
        }

        return theResult;

    }

    private static final int findInFile(String aFileName, String aSearchFor) {
        int theBufferSize = 5242880; // 5MB
        boolean theSearchOn = true;
        String theStringBuffer = null;
        int theOffset = 0;
        int theRead = theBufferSize;
        int thePosition = 0;
        int theOverhead = aSearchFor.length() - 1;
        int theResult = -1;

        if (theBufferSize >= aSearchFor.length()) {
            try {
                File file = new File(aFileName);
                RandomAccessFile ra = new RandomAccessFile(aFileName, "r");
                byte[] theByteBuffer = new byte[(int)theBufferSize];

                while ((theOffset < file.length()) && (theSearchOn) && (theRead == theBufferSize)) {
                    theRead = ra.read(theByteBuffer);

                    if (theRead >= 0) {
                        theStringBuffer = new String(theByteBuffer, 0, theRead);

                        thePosition = theStringBuffer.indexOf(aSearchFor);

                        if (thePosition >= 0) {
                            theResult = theOffset + thePosition;
                            theSearchOn = false;
                            LOGGER.debug("Found '" + aSearchFor + "' in '" + aFileName + "' at position " + theResult);
                        } else {
                            if (theRead == theBufferSize) {
                                theOffset += (theRead - theOverhead);
                                ra.seek(theOffset);
                            }
                        }
                    }
                }

                if (ra != null) {
                    ra.close();
                }
            } catch (FileNotFoundException ex) {
                LOGGER.error("Cannot find database file " + aFileName, ex);
            } catch (IOException ex) {
                LOGGER.error("Cannot read database file " + aFileName, ex);
            }
        } else {
            throw new RuntimeException("The string to find is too long. Only strings of lenght up to " + theBufferSize + " can be found!");
        }

        return theResult;
    }

    /**
     * Expands a given string by inserting special characters between
     * each original character.
     *
     * @param aString           - the string to expand
     * @param aDividingCharCode - the character to use for expanding
     *
     * @return An expanded string
     */
    private static String expand(String aString, Integer aDividingCharCode) {
        StringBuilder buffer = new StringBuilder(aString);

        for (int i = 0; i < aString.length() - 1; i++) {
            buffer.insert((i * 2) + 1, (char)(int)aDividingCharCode);
        }

        return buffer.toString();
    }

}