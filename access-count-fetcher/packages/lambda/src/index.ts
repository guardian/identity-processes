import { set, sub } from 'date-fns';
import { formatInTimeZone } from 'date-fns-tz';
import { Redis } from 'ioredis';
import type { Config } from './config';
import { getConfig } from './config';
import { s3Client } from './lib/aws';
import type { MetricStorage } from './MetricStorage';
import { S3MetricStorage } from './MetricStorage';

export type CountData = {
	client: string;
	method: string;
	url: string;
	periodStart: Date;
	count: number;
};

//TODO see how to refactor the code so that it's accessible from unit tests without having to export everything
export function _getStartOfPeriodThatContains(date: Date): Date {
	const minutes = date.getMinutes();
	const minutesSincePeriodStart = minutes % 5; //TODO extract the 5 to a constant
	const dateWithMinutePrecision = set(date, { seconds: 0, milliseconds: 0 });
	const periodStartDate = sub(dateWithMinutePrecision, {
		minutes: minutesSincePeriodStart,
	});
	return periodStartDate;
}
export async function _run_main(
	config: Config,
	redis: Redis,
	fetchCurrentTime: () => Date,
	metricStorage: MetricStorage,
) {
	console.log(`starting ${config.app} ${config.stage}`);
	async function deleteKeys(keys: string[]) {
		const pipeline = redis.pipeline();
		console.log(`deleting ${keys.length} keys`);
		keys.forEach(function (key) {
			pipeline.del(key);
		});
		await pipeline.exec();
	}
	const DATE_FORMAT = 'yyyy-MM-dd_HH:mm';

	async function persist(periodStart: Date, keys: string[]): Promise<void> {
		console.log(`saving ${keys.length} keys to db`);
		for (const key of keys) {
			const value = await redis.get(key);
			if (value) {
				const parts = key.split('::');
				if (parts.length === 5) {
					const [, , client, method, url] = parts;
					const countData: CountData = {
						periodStart: periodStart,
						client,
						method,
						url,
						count: +value,
					};
					await metricStorage.putData(countData);
				} else {
					console.error(`invalid key format ${key}`);
				}
			} else {
				console.error(`undefined val for ${key}`);
			}
		}
	}

	async function process(periodStart: Date, keys: string[]): Promise<void> {
		await persist(periodStart, keys);
		await deleteKeys(keys);
	}

	const currentPeriodStart = _getStartOfPeriodThatContains(fetchCurrentTime());
	const previousPeriodStart = sub(currentPeriodStart, { minutes: 5 });
	const formattedPreviousPeriodStart = formatInTimeZone(
		previousPeriodStart,
		'UTC',
		DATE_FORMAT,
	);

	const scanOptions = {
		match: `gu-count::${formattedPreviousPeriodStart}*`,
		// returns approximately 500 elements per call
		count: 500,
	};

	console.log(`processing keys with prefix: ${scanOptions.match}`);

	const stream = redis.scanStream(scanOptions);

	return new Promise((resolve, reject) => {
		stream.on('data', (resultKeys: string[]) => {
			stream.pause();
			process(previousPeriodStart, resultKeys)
				.then(() => {
					return stream.resume();
				})
				.catch((error) => {
					redis.disconnect();
					reject(error);
				});
		});

		stream.on('end', () => {
			console.log('done!');
			redis.disconnect();
			metricStorage.onFinish().then(
				(success) => resolve('OK'),
				(error) => reject(error),
			);
		});
	});
}
export async function main() {
	const config = await getConfig();
	const redis = new Redis({
		host: config.redis_host,
		port: config.redis_port,
		password: config.redis_password,
		tls: config.redis_sslOn ? {} : undefined,
	});
	const metricStorage = new S3MetricStorage(
		s3Client,
		config.output_bucket,
		config.output_bucket_base_path,
	);

	return _run_main(config, redis, () => new Date(), metricStorage);
}
