import 'source-map-support/register';
import { GuRootExperimental } from '@guardian/cdk/lib/experimental/constructs';
import { AccessCountFetcher } from '../lib/access-count-fetcher';

const app = new GuRootExperimental();

new AccessCountFetcher(app, 'AccessCountFetcher-PROD', {
	stack: 'identity',
	stage: 'PROD',
	env: {
		region: 'eu-west-1',
	},
});

new AccessCountFetcher(app, 'AccessCountFetcher-CODE', {
	stack: 'identity',
	stage: 'CODE',
	env: {
		region: 'eu-west-1',
	},
});
