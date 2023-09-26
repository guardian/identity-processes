import { GuScheduledLambda } from '@guardian/cdk';
import type { GuStackProps } from '@guardian/cdk/lib/constructs/core';
import { GuStack } from '@guardian/cdk/lib/constructs/core';
import type { App } from 'aws-cdk-lib';
import { Duration } from 'aws-cdk-lib';
import { Architecture, Runtime } from 'aws-cdk-lib/aws-lambda';

export class AccessCountFetcher extends GuStack {
	constructor(scope: App, id: string, props: GuStackProps) {
		super(scope, id, props);

		new GuScheduledLambda(this, 'AccessCountFetcher', {
			monitoringConfiguration: { noMonitoring: true },
			app: 'access-count-fetcher',
			fileName: 'access-count-fetcher.zip',
			handler: 'index.main',
			runtime: Runtime.NODEJS_18_X,
			architecture: Architecture.ARM_64,
			timeout: Duration.minutes(5),
			memorySize: 128,
			rules: [
				// {
				// 	schedule: Schedule.cron({
				// 		day: '*',
				// 		hour: '*',
				// 		minute: '*/5',
				// 	}),
				// 	description: 'fetch idapi usage metrics',
				// },
			],
		});
	}
}
