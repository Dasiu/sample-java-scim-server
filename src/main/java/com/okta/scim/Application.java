/** Copyright © 2018, Okta, Inc.
 * 
 *  Licensed under the MIT license, the "License";
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *     https://opensource.org/licenses/MIT
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.okta.scim;

import java.util.UUID;

import javax.annotation.PostConstruct;

import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import com.github.javafaker.Faker;
import com.okta.scim.database.GroupDatabase;
import com.okta.scim.database.GroupMembershipDatabase;
import com.okta.scim.database.UserDatabase;
import com.okta.scim.dispatchers.LoggingDispatcherServlet;
import com.okta.scim.models.Group;
import com.okta.scim.models.GroupMembership;
import com.okta.scim.models.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;

@SpringBootApplication
@EnableSwagger2
public class Application {
    private static Logger logger = LoggerFactory.getLogger(Application.class);

    @Autowired
    UserDatabase userDb;
    @Autowired
    GroupDatabase groupDb;
    @Autowired
    GroupMembershipDatabase groupMembershipDatabase;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        logger.info("SCIM server running...");
    }

    @Bean
    public ServletRegistrationBean dispatcherRegistration() {
        return new ServletRegistrationBean(dispatcherServlet());
    }

    @Bean(name = DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
    public DispatcherServlet dispatcherServlet() {
        return new LoggingDispatcherServlet();
    }

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build();
    }

    @PostConstruct
    public void init() {
        Faker faker = new Faker();

        Group group = generateRandomGroup(faker);

        for (int i = 0; i < 103; i++) {
            User user = generateRandomUser(faker);

            generateMembership(group, user);
        }


    }

    private void generateMembership(Group group, User user) {
        GroupMembership groupMembership = new GroupMembership();
        groupMembership.id = UUID.randomUUID().toString();
        groupMembership.userId = user.id;
        groupMembership.groupId = group.id;
        groupMembership.groupDisplay = group.displayName;
        groupMembership.userDisplay = user.userName;

        groupMembershipDatabase.save(groupMembership);
    }

    private Group generateRandomGroup(Faker faker) {
        Group group = new Group();
        group.displayName = faker.lebowski().character();
        group.id = UUID.randomUUID().toString();

        return groupDb.save(group);
    }

    private User generateRandomUser(Faker faker) {
        User user = new User();
        user.id = UUID.randomUUID().toString();
        user.userName = "username_" + user.id;
        user.active = true;
        user.familyName = faker.name().firstName();
        user.givenName = faker.name().lastName();
        user.middleName = faker.name().nameWithMiddle();

        return userDb.save(user);
    }
}
