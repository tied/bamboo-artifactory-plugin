[@ui.bambooSection titleKey='Artifactory Generic Deploy']
[@ww.select name='artifactory.generic.artifactoryServerId' labelKey='Artifactory Server URL' list=serverConfigManager.allServerConfigs
listKey='id' listValue='url' onchange='javascript: displayGenericArtifactoryConfigs(this.value)' emptyOption=true toggle='true'
descriptionKey='Select an Artifactory server.'/]
<div id="genericArtifactoryConfigDiv">
[@ww.select name='artifactory.generic.deployableRepo' labelKey='Target Repository' list=dummyList
listKey='repoKey' listValue='repoKey' toggle='true' descriptionKey='Select a target deployment repository.'/]
[@ww.textfield name='artifactory.generic.username' labelKey='Deployer username' descriptionKey='User with deploy permissions to Artifactory'/]
[@ww.password name='artifactory.generic.password' labelKey='Deployer password' descriptionKey='Password with deploy permissions to Artifactory' showPassword='true'/]
[@ww.textarea name='artifactory.generic.deployPattern' label='Edit Published Artifacts' rows='10' cols='80'
description='New line or comma separated paths to build artifacts that will be published to Artifactory. Supports Ant-style<br/>
wildcards mapping to target directories. E.g.:<br/>
<b>**/*.zip=>winFiles</b> - Deploys all zip files under the working directory to the winFiles directory of the target
repository, maintaining the original relative path for each file.<br/>
<b>unix/*.tgz</b> - Deploys all tgz files under the unix directory to the root directory of the target repository,
maintaining the original relative path for each file.<br/>
<b>unix/**=>/ux-x64</b> - Deploys all files under the unix directory to the ux-x64 directory of the target repository,
as a flat list of files.<br/>
are target directories.' cssClass="long-field" /]
</div>
[/@ui.bambooSection ]

<script>
    function displayGenericArtifactoryConfigs(serverId) {
        var configDiv = document.getElementById('genericArtifactoryConfigDiv');
        if ((serverId == null) || (serverId.length == 0) || (-1 == serverId)) {
            configDiv.style.display = 'none';
        } else {
            configDiv.style.display = 'block';
            var urlSelect = document.getElementsByName('artifactory.generic.artifactoryServerId')[0];
            var urlOptions = urlSelect.options;
            for (var i = 0; i < urlOptions.length; i++) {
                var option = urlOptions[i];
                if (option.value == '' + serverId) {
                    urlSelect.selectedIndex = i;
                    break;
                }
            }

            loadGenericRepoKeys(serverId)
        }
    }

    function loadGenericRepoKeys(serverId) {
        AJS.$.ajax({
            url: '${req.contextPath}/plugins/servlet/artifactoryConfigServlet?serverId=' + serverId +
                    '&deployableRepos=true',
            dataType: 'json',
            cache: false,
            success: function (json) {
                var repoSelect = document.getElementsByName('artifactory.generic.deployableRepo')[0];
                repoSelect.innerHTML = '';
                if (serverId >= 0) {

                    var selectedRepoKey = '${selectedRepoKey}';

                    for (var i = 0, l = json.length; i < l; i++) {
                        var deployableRepoKey = json[i];
                        var option = document.createElement('option');
                        option.innerHTML = deployableRepoKey;
                        option.value = deployableRepoKey;
                        repoSelect.appendChild(option);
                        if (selectedRepoKey && (deployableRepoKey == selectedRepoKey)) {
                            repoSelect.selectedIndex = i;
                        }
                    }
                }
            },
            error : function (XMLHttpRequest, textStatus, errorThrown) {
                var errorMessage = 'An error has occurred while retrieving the target repository list.\n' +
                        'Response: ' + XMLHttpRequest.status + ', ' + XMLHttpRequest.statusText + '.\n';
                if (XMLHttpRequest.status == 404) {
                    errorMessage +=
                            'Please make sure that the Artifactory Server Configuration Management Servlet is accesible.'
                } else {
                    errorMessage +=
                            'Please check the server logs for error messages from the Artifactory Server Configuration Management Servlet.'
                }
                alert(errorMessage);
            }
        });
    }
    displayGenericArtifactoryConfigs(${selectedServerId});
</script>