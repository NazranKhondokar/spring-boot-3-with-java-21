package com.nazran.batchprocess.config;

import com.nazran.batchprocess.entity.Employee;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.ResultSet;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Bean
    public JdbcCursorItemReader<Employee> reader(DataSource sourceDataSource) {
        return new JdbcCursorItemReaderBuilder<Employee>()
                .dataSource(sourceDataSource)
                .name("employeeItemReader")
                .sql("SELECT id, first_name, last_name, department FROM employee")
                .rowMapper((ResultSet rs, int rowNum) -> {
                    return new Employee(
                            rs.getLong("id"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getString("department")
                    );
                })
                .build();
    }

    @Bean
    public EmployeeProcessor processor() {
        return new EmployeeProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<Employee> writer(DataSource destinationDataSource) {
        return new JdbcBatchItemWriterBuilder<Employee>()
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql("INSERT INTO employee (first_name, last_name, department) VALUES (:firstName, :lastName, :department)")
                .dataSource(destinationDataSource)
                .build();
    }

    @Bean
    public Job importEmployeeJob(JobBuilderFactory jobBuilderFactory, Step step1) {
        return jobBuilderFactory.get("importEmployeeJob")
                .incrementer(new RunIdIncrementer())
                .flow(step1)
                .end()
                .build();
    }

    @Bean
    public Step step1(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("step1")
                .<Employee, Employee>chunk(10)
                .reader(reader(null))  // Source Database Reader
                .processor(processor())
                .writer(writer(null))  // Destination Database Writer
                .build();
    }
}

