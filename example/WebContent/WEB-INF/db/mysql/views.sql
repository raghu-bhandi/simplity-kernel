USE `petclinic`;

DROP VIEW IF EXISTS `petclinic`.`pet_details` ;

CREATE VIEW `petclinic`.`pet_details` AS
    SELECT 
        `a`.`id` AS `pet_id`,
        `a`.`name` AS `pet_name`,
        `a`.`birth_date` AS `birth_date`,
        `a`.`type_id` AS `type_id`,
        `a`.`owner_id` AS `owner_id`,
        `b`.`name` AS `pet_type`,
        `c`.`first_name` AS `first_name`,
        `c`.`last_name` AS `last_name`
    FROM
        ((`petclinic`.`pets` `a`
        JOIN `petclinic`.`types` `b`)
        JOIN `petclinic`.`owners` `c`)
    WHERE
        ((`a`.`type_id` = `b`.`id`)
            AND (`a`.`owner_id` = `c`.`id`))
DROP VIEW IF EXISTS `petclinic`.`vet_specialties_details` ;
CREATE VIEW `petclinic`.`vet_specialties_details` AS
    SELECT 
        `a`.`vet_id` AS `vet_id`,
        `a`.`specialty_id` AS `specialty_id`,
        `b`.`name` AS `specialty`
    FROM
        (`petclinic`.`vet_specialties` `a`
        JOIN `petclinic`.`specialties` `b`)
    WHERE
        (`a`.`specialty_id` = `b`.`id`);

