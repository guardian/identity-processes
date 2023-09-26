import * as process from 'process';
import { getParameter } from './lib/aws';

interface EnvConfig {
	stack: string;
	stage: string;
	app: string;
}

interface ParameterStoreConfig {
	redis_host: string;
	redis_password: string;
	redis_port: number;
	redis_sslOn: boolean;
	output_bucket: string;
	output_bucket_base_path: string;
}

export type Config = EnvConfig & ParameterStoreConfig;

function getEnvOrThrow(key: string): string {
	const value = process.env[key];
	if (value === undefined) {
		throw new Error(`Environment variable ${key} is not set`);
	}
	return value;
}

export async function getConfig(): Promise<Config> {
	const envConfig: EnvConfig = {
		stack: getEnvOrThrow('STACK'),
		stage: getEnvOrThrow('STAGE').toUpperCase(),
		app: getEnvOrThrow('APP'),
	};
	const parameterName = `/${envConfig.stage}/${envConfig.stack}/${envConfig.app}/config`;
	console.log(`fetching configuration from parameter ${parameterName}`);
	const config_str = await getParameter(parameterName, true);
	const paramConfig = JSON.parse(config_str) as ParameterStoreConfig;
	// Ensure that the config object has all the required properties.
	if (
		!paramConfig.redis_host ||
		!paramConfig.redis_password ||
		!paramConfig.output_bucket ||
		!paramConfig.output_bucket_base_path ||
		!paramConfig.redis_port ||
		typeof paramConfig.redis_sslOn === 'undefined'
	) {
		throw new Error('Config missing required properties');
	}
	return { ...envConfig, ...paramConfig };
}
