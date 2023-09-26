import zlib from 'zlib';
import { PutObjectCommand } from '@aws-sdk/client-s3';
import type { S3Client } from '@aws-sdk/client-s3';
import { formatInTimeZone } from 'date-fns-tz';
import type { CountData } from './index';

export interface MetricStorage {
	// send data
	putData(countData: CountData): Promise<void>;
	// called when we are done putting data
	onFinish(): Promise<void>;
}
export type S3MetricStorageConfig = {
	s3Client: S3Client;
	bucket: string;
	keyPrefix: string;
};
export class S3MetricStorage {
	s3Client: S3Client;
	bucket: string;
	keyPrefix: string;
	//we don't want to make a request per single metric, we keep them all in this array and write them all at the same time
	countDataBuffer: CountData[];
	constructor(s3Client: S3Client, bucket: string, keyPrefix: string) {
		this.s3Client = s3Client;
		this.bucket = bucket;
		this.keyPrefix = keyPrefix;
		this.countDataBuffer = [];
	}

	// send data
	putData(countData: CountData): Promise<void> {
		this.countDataBuffer.push(countData);
		return Promise.resolve();
	}

	async onFinish(): Promise<void> {
		// todo maybe use multipart to generate bigger files that show up less frequently

		//assumes that everything in the buffer is for the same period which should be true
		if (this.countDataBuffer.length > 0) {
			const period_date = this.countDataBuffer[0].periodStart;
			const periodDateString = formatInTimeZone(
				period_date,
				'UTC',
				'yyyy-MM-dd_HH-mm',
			);
			const key = `${this.keyPrefix}/access_${periodDateString}.csv.gz`;
			//date, client, method, url, count
			const bodyLines: string[] = this.countDataBuffer.map((d) => {
				//todo handle special characters and new lines and stuff (maybe just use a csv library
				return `${d.periodStart.toISOString()},"${d.client}","${d.method}","${
					d.url
				}",${d.count}`;
			});
			bodyLines.unshift('period_start,client,method,url,count');
			const body = bodyLines.join('\n');
			console.log(`writing to ${key} `);
			const zippedBody = zlib.gzipSync(body);
			const putObjectCommand = new PutObjectCommand({
				Bucket: this.bucket,
				Key: key,
				Body: zippedBody,
			});
			await this.s3Client.send(putObjectCommand);
		} else return Promise.resolve();
	}
}
