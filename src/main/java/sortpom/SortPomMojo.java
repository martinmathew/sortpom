package sortpom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.jdom.JDOMException;

import se.mine.mindif.Context;
import se.mine.mindif.Dependency;
import sortpom.util.FileUtil;
import sortpom.util.LineSeparator;

/**
 *
 * @author Bjorn
 * @goal sort
 */
public class SortPomMojo extends AbstractMojo {
	/**
	 * This is the File instance that refers to the location of the POM that should be sorted.
	 *
	 * @parameter expression="${sort.pomFile}" default-value="${project.file}"
	 */
	private File pomFile;

	/**
	 * Should a backup copy be created for the sorted pom.
	 *
	 * @parameter expression="${sort.createBackupFile}" default-value="true"
	 */
	private boolean createBackupFile;

	/**
	 * Name of the file extension for the backup file.
	 *
	 * @parameter expression="${sort.backupFileExtension}" default-value=".bak"
	 */
	private String backupFileExtension;

	/**
	 * Encoding for the files
	 *
	 * @parameter expression="${sort.encoding}" default-value="UTF-8"
	 */
	private String encoding;

	/**
	 * Line separator for sorted pom. Can be either \n, \r or \r\n
	 *
	 * @parameter expression="${sort.lineSeparator}" default-value="${line.separator}"
	 */
	private String lineSeparatorString;

	/**
	 * Custom sort order file
	 *
	 * @parameter expression="${sort.sortOrderFile}"
	 */
	private File defaultOrderFileName;

	@Dependency
	private FileUtil fileUtil;

	@Dependency
	private XmlProcessor xmlProcessor;

	@Override
	public void execute() throws MojoFailureException {
		new Context().inject(this);
		final LineSeparator lineSeparator = new LineSeparator(lineSeparatorString);
		fileUtil.setup(pomFile, backupFileExtension, encoding, defaultOrderFileName);
		getLog().info("Sorting file " + pomFile.getAbsolutePath());

		String xml = fileUtil.getPomFileContent();
		String sortedXml = getSortedXml(lineSeparator, xml);
		if (xml.replaceAll("\\n|\\r", "").equals(sortedXml.replaceAll("\\n|\\r", ""))) {
			getLog().info("Pomfile is already sorted, exiting");
			return;
		}
		createBackupFile();
		saveSortedPomFile(sortedXml);
	}

	private void createBackupFile() throws MojoFailureException {
		if (createBackupFile) {
			if (backupFileExtension.trim().length() == 0) {
				throw new MojoFailureException("Could not create backup file, extension name was empty");
			}
			fileUtil.backupFile();
			getLog().info(
					"Saved backup of " + pomFile.getAbsolutePath() + " to " + pomFile.getAbsolutePath()
							+ backupFileExtension);
		}
	}

	private String getSortedXml(final LineSeparator lineSeparator, final String xml) throws MojoFailureException {
		ByteArrayInputStream originalXmlInputStream = null;
		ByteArrayOutputStream sortedXmlOutputStream = null;
		try {
			originalXmlInputStream = new ByteArrayInputStream(xml.getBytes(fileUtil.getEncoding()));
			xmlProcessor.setOriginalXml(originalXmlInputStream);
			xmlProcessor.sortXml();
			sortedXmlOutputStream = new ByteArrayOutputStream();
			xmlProcessor.getSortedXml(lineSeparator, sortedXmlOutputStream);
			return sortedXmlOutputStream.toString(fileUtil.getEncoding());
		} catch (JDOMException e) {
			throw new MojoFailureException("Could not sort pomfiles content: " + xml, e);
		} catch (IOException e) {
			throw new MojoFailureException("Could not sort pomfiles content: " + xml, e);
		} finally {
			IOUtils.closeQuietly(originalXmlInputStream);
			IOUtils.closeQuietly(sortedXmlOutputStream);
		}

	}

	private void saveSortedPomFile(final String sortedXml) throws MojoFailureException {
		fileUtil.savePomFile(sortedXml);
		getLog().info("Saved sorted pomfile to " + pomFile.getAbsolutePath());
	}
}