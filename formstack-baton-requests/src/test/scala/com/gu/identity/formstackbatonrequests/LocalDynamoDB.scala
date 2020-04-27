package com.gu.identity.formstackbatonrequests

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClient}
import com.amazonaws.services.dynamodbv2.model.{AttributeDefinition, CreateTableResult, KeySchemaElement, KeyType, ProvisionedThroughput, ScalarAttributeType}

object LocalDynamoDB {

  import scala.collection.JavaConverters._

  def client(): AmazonDynamoDB =
    AmazonDynamoDBClient
      .builder()
      .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("dummy", "credentials")))
      .withEndpointConfiguration(new EndpointConfiguration("http://localhost:8000", ""))
      .build()

  def createTable(client: AmazonDynamoDB)(tableName: String)(
    attributes: (Symbol, ScalarAttributeType)*): CreateTableResult = {
    client.createTable(
      attributeDefinitions(attributes),
      tableName,
      keySchema(attributes),
      arbitraryThroughputThatIsIgnoredByDynamoDBLocal
    )
  }

  private def keySchema(attributes: Seq[(Symbol, ScalarAttributeType)]) = {
    val hashKeyWithType :: rangeKeyWithType = attributes.toList
    val keySchemas = hashKeyWithType._1 -> KeyType.HASH :: rangeKeyWithType.map(_._1 -> KeyType.RANGE)
    keySchemas.map { case (symbol, keyType) => new KeySchemaElement(symbol.name, keyType) }.asJava
  }

  private def attributeDefinitions(attributes: Seq[(Symbol, ScalarAttributeType)]) = {
    attributes.map { case (symbol, attributeType) => new AttributeDefinition(symbol.name, attributeType) }.asJava
  }

  private val arbitraryThroughputThatIsIgnoredByDynamoDBLocal = new ProvisionedThroughput(1L, 1L)
}