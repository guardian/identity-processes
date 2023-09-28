module.exports = {
	verbose: true,
	testEnvironment: 'node',
	projects: [
		{
			displayName: 'lambda',
			transform: {
				'^.+\\.tsx?$': 'ts-jest',
			},
			testMatch: ['<rootDir>/packages/lambda/**/*.test.ts'],
			setupFiles: ['dotenv/config'],
		},
	],
};
