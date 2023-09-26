import Redis from 'ioredis-mock';
import type { Config } from './config';
import type { MetricStorage } from './MetricStorage';
import type { CountData } from './index';
import { _getStartOfPeriodThatContains, _run_main } from './index';

describe('The lambda', () => {
	beforeAll(() => {
		// Override the STAGE value from `.env`
		process.env.STAGE = 'TEST';
		process.env.APP = 'access-count-checker';
	});

	function fakeDate() {
		return new Date('2023-11-23T23:31:00');
	}
	it('should calculate start of 5 minute period for date in the middle of the period', () => {
		// For simplicity 5 minute periods are delimited by dates with 0 seconds and minutes that end in 0 or 5
		const date1 = new Date('2023-11-23T23:12:20.123');
		const expectedPeriodEnd1 = new Date('2023-11-23T23:10:00.000');
		expect(_getStartOfPeriodThatContains(date1)).toEqual(expectedPeriodEnd1);

		const date2 = new Date('2023-11-23T23:58:38.123');
		const expectedPeriodEnd2 = new Date('2023-11-23T23:55:00.000');
		expect(_getStartOfPeriodThatContains(date2)).toEqual(expectedPeriodEnd2);
	});

	it('should calculate start of 5 minute period dates at first or last minute of the period', () => {
		// if the date minutes end in 0 or 5 the start of the period would be the start of that same minute
		const date1 = new Date('2023-11-23T23:10:50.300');
		const expectedPeriodEnd1 = new Date('2023-11-23T23:10:00.000');
		expect(_getStartOfPeriodThatContains(date1)).toEqual(expectedPeriodEnd1);

		const date2 = new Date('2023-11-23T23:55:38.123');
		const expectedPeriodEnd2 = new Date('2023-11-23T23:55:00.000');
		expect(_getStartOfPeriodThatContains(date2)).toEqual(expectedPeriodEnd2);
	});

	// with the current logic changing days or hours shouldn't make a difference as the periods are aligned to dates with minutes multiple of 5
	it('should calculate start of 5 minute period dates at start of a day', () => {
		const date1 = new Date('2023-11-23T23:59:30.031');
		const expectedPeriodEnd1 = new Date('2023-11-23T23:55:00.000');
		expect(_getStartOfPeriodThatContains(date1)).toEqual(expectedPeriodEnd1);
	});

	it('should fetch keys, save to db and delete', async () => {
		const redis = new Redis();
		const lastPeriodKey1 =
			'gu-count::2023-11-23_23:25::client-id-1::GET::^/someUrl/([^/]+)$';
		const lastPeriodKey2 =
			'gu-count::2023-11-23_23:25::client-id-2::POST::^/anotherUrl/(\\\\d+?)$';
		const currentPeriodKey =
			'gu-count::2023-11-23_23:30::client-id-2::POST::^/anotherUrl/(\\\\d+?)$';

		//set 2 counter keys from the past period that should be processed and one from the current period that should be left alone
		await redis.set(lastPeriodKey1, 231);
		await redis.set(lastPeriodKey2, 3);
		//this key should be left alone as the counting period is not over
		await redis.set(currentPeriodKey, 15);

		class MockStorageMetrics implements MetricStorage {
			onFinish() {
				return Promise.resolve();
			}
			putData(countdata: CountData): Promise<void> {
				return Promise.resolve();
			}
		}
		const mockStorageMetrics = new MockStorageMetrics();
		const mockPutData = jest.spyOn(mockStorageMetrics, 'putData');

		const config: Config = {
			stage: 'DEV',
			app: 'access-count-fetcher',
			stack: 'identity',
			redis_host: 'fake_redis_host',
			redis_password: 'fake_redis_password',
			redis_sslOn: false,
			redis_port: 1234,
			output_bucket: 'fake_output_bucket',
			output_bucket_base_path: 'fake_output_basePath',
		};
		const response = await _run_main(
			config,
			redis,
			fakeDate,
			mockStorageMetrics,
		);
		expect(response).toEqual('OK');
		expect(mockPutData.mock.calls).toHaveLength(2);

		const startOfPreviousPeriod = new Date('2023-11-23T23:25:00');
		expect(mockPutData.mock.calls[0][0]).toEqual({
			count: 231,
			client: 'client-id-1',
			method: 'GET',
			periodStart: startOfPreviousPeriod,
			url: '^/someUrl/([^/]+)$',
		});
		expect(mockPutData.mock.calls[1][0]).toEqual({
			count: 3,
			client: 'client-id-2',
			method: 'POST',
			periodStart: startOfPreviousPeriod,
			url: '^/anotherUrl/(\\\\d+?)$',
		});
		//it should have removed the keys that it processed ..
		await expect(redis.get(lastPeriodKey1)).resolves.toBe(null);
		await expect(redis.get(lastPeriodKey2)).resolves.toBe(null);
		//but left alone the keys from the current period
		await expect(redis.get(currentPeriodKey)).resolves.toBe('15');
	});
});
