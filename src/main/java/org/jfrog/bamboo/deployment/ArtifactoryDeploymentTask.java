package org.jfrog.bamboo.deployment;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskType;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.runtime.RuntimeTaskDefinition;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.util.BambooBuildInfoLog;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.bamboo.util.deployment.FilesCollector;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.client.DeployDetails;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Bamboo deployment Artifactory task - Takes pre defined artifacts from a build plan and deploys them to Artifactory
 *
 * @author Aviad Shikloshi
 */
public class ArtifactoryDeploymentTask implements DeploymentTaskType {

    private static final Logger log = Logger.getLogger(ArtifactoryDeploymentTask.class);

    private ServerConfig serverConfig;
    private String repositoryKey;
    private Multimap<String, String> artifactSpecs;
    private String artifactsRootDirectory;
    private BuildLogger buildLogger;
    private ArtifactoryBuildInfoClient client;

    @NotNull
    @Override
    public TaskResult execute(@NotNull DeploymentTaskContext deploymentTaskContext) throws TaskException {

        buildLogger = deploymentTaskContext.getBuildLogger();
        ServerConfigManager serverConfigManager = ServerConfigManager.getInstance();
        final String serverId = deploymentTaskContext.getConfigurationMap().get("artifactoryServerId");
        serverConfig = serverConfigManager.getServerConfigById(Long.parseLong(serverId));
        if (serverConfig == null) {
            buildLogger.addErrorLogEntry("Please check Artifactory server configuration in the job configuration.");
            return TaskResultBuilder.newBuilder(deploymentTaskContext).failedWithError().build();
        }

        String matrixParamStr = deploymentTaskContext.getConfigurationMap().get(ArtifactoryDeploymentConfiguration.MATRIX_PARAM);
        repositoryKey = deploymentTaskContext.getConfigurationMap().get(ArtifactoryDeploymentConfiguration.DEPLOYMENT_REPOSITORY);
        artifactSpecs = TaskUtils.extractMatrixParamFromString(matrixParamStr);
        RuntimeTaskDefinition artifactDownloadTask = TaskUtils.findDownloadArtifactsTask(deploymentTaskContext.getCommonContext().getRuntimeTaskDefinitions());
        artifactsRootDirectory = deploymentTaskContext.getRootDirectory().getAbsolutePath();
        FilesCollector filesCollector = new FilesCollector(artifactsRootDirectory, artifactDownloadTask);

        TaskResult result;

        ArtifactoryBuildInfoClient client = new ArtifactoryBuildInfoClient(serverConfig.getUrl(),
                serverConfig.getUsername(), serverConfig.getPassword(), new BambooBuildInfoLog(log));

        try {
            Map<String, Set<File>> artifacts = filesCollector.getCollectedFiles();
            Set<DeployDetails> deployDetailsSet = createDeploymentDetailsForArtifacts(artifacts);
            deploy(deployDetailsSet);
            result = TaskResultBuilder.newBuilder(deploymentTaskContext).success().build();
        } catch (Exception e) {
            buildLogger.addErrorLogEntry("Error while deploying artifacts to Artifactory: " + e.getMessage());
            result = TaskResultBuilder.newBuilder(deploymentTaskContext).failedWithError().build();
        }
        client.shutdown();
        return result;
    }

    /**
     * Deploy all collected artifacts to Artifactory
     *
     * @param deployDetailsSet details for the artifacts we want to deploy
     * @throws IOException
     */
    private void deploy(Set<DeployDetails> deployDetailsSet) throws IOException {
        for (DeployDetails deployDetails : deployDetailsSet) {
            client.deployArtifact(deployDetails);
            buildLogger.addBuildLogEntry("Deployed: " + deployDetails.getArtifactPath() + " to: " + deployDetails.getTargetRepository());
        }
    }


    /**
     * Create DeployDetails for all the collected artifacts
     *
     * @param artifacts files to be uploaded to Artifactory
     * @return set of all deployment details
     */
    private Set<DeployDetails> createDeploymentDetailsForArtifacts(Map<String, Set<File>> artifacts) {
        Set<DeployDetails> deployDetailList = Sets.newHashSet();
        for (String path : artifacts.keySet()) {
            Set<File> filesForPath = artifacts.get(path);
            for (File file : filesForPath) {
                deployDetailList.add(createDeployDetailsForOneArtifact(file, path));
            }
        }
        return deployDetailList;
    }

    /**
     * Create DeploymentDetails for artifact
     *
     * @param artifact artifact file object
     * @return DeploymentDetails for artifact
     */
    private DeployDetails createDeployDetailsForOneArtifact(File artifact, String pathToArtifact) {
        DeployDetails.Builder deployDetailsBuilder = new DeployDetails.Builder();
        try {
            Map<String, String> checksum = FileChecksumCalculator.calculateChecksums(artifact, "SHA1", "MD5");
            deployDetailsBuilder
                    .artifactPath(createArtifactPath(artifact.getPath(), pathToArtifact))
                    .file(artifact)
                    .targetRepository(repositoryKey)
                    .sha1(checksum.get("SHA1"))
                    .md5(checksum.get("MD5"))
                    .addProperties((ArrayListMultimap<String, String>) artifactSpecs);
            return deployDetailsBuilder.build();
        } catch (Exception e) {
            throw new RuntimeException("Error while creating Artifact details", e);
        }
    }

    /**
     * Create the artifact path in artifactory
     *
     * @param artifactPath full artifact path on file system
     * @return the calculated path to deploy to inside the repository
     */
    private String createArtifactPath(String artifactPath, String subDir) {
        int numberOfSeparators = StringUtils.isBlank(subDir) ? 1 : 2;
        int start = artifactsRootDirectory.length() + subDir.length() + numberOfSeparators;
        return StringUtils.substring(artifactPath, start).replace("\\", "/");
    }
}