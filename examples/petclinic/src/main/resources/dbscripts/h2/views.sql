CREATE VIEW `pet_details` AS
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
        ((`pets` `a`
        JOIN `types` `b`)
        JOIN `owners` `c`)
    WHERE
        ((`a`.`type_id` = `b`.`id`)
            AND (`a`.`owner_id` = `c`.`id`));
			
CREATE VIEW `vet_specialties_details` AS
    SELECT 
        `a`.`vet_id` AS `vet_id`,
        `a`.`specialty_id` AS `specialty_id`,
        `b`.`name` AS `specialty`
    FROM
        (`vet_specialties` `a`
        JOIN `specialties` `b`)
    WHERE
        (`a`.`specialty_id` = `b`.`id`);

