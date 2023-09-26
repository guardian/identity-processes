import { S3Client } from '@aws-sdk/client-s3';
import { GetParameterCommand, SSMClient } from '@aws-sdk/client-ssm';
import { GetParameterCommandOutput } from '@aws-sdk/client-ssm/dist-types/commands';
import { fromNodeProviderChain } from '@aws-sdk/credential-providers';

const AWS_REGION = 'eu-west-1';
const CREDENTIAL_PROVIDER = fromNodeProviderChain({
	profile: 'identity',
});
export const ssmClient = new SSMClient({
	region: AWS_REGION,
	credentials: CREDENTIAL_PROVIDER,
});

export const s3Client = new S3Client({
	region: AWS_REGION,
	credentials: CREDENTIAL_PROVIDER,
});
export const getParameter = async (
	name: string,
	withDecryption = false,
): Promise<string> => {
	const command = new GetParameterCommand({
		Name: name,
		WithDecryption: withDecryption,
	});
	const response = await ssmClient.send(command);
	if (!response.Parameter?.Value) {
		throw new Error(`Failed to retrieve parameter ${name}`);
	}
	return response.Parameter.Value;
};
