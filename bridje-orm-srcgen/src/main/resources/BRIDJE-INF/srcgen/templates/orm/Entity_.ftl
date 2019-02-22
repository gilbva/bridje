
package ${entity.model.package};

import java.io.Serializable;
import java.sql.JDBCType;
import org.bridje.orm.*;
import org.bridje.sql.*;
import java.util.Objects;
import javax.annotation.Generated;

/**
 * This class represents the ${entity.name} entity SQL objects.
 * ${entity.description!}
 */
@Generated(value = "org.bridje.orm.srcgen.OrmSourceGenerator", date = "${.now?string("yyyy-MM-dd")}", comments = "Generated by Bridje ORM API")
class ${entity.name}_
{
    static final SQLType<${entity.name}, ${entity.key.type.readType}> RELATION_TYPE;

    /**
     * This static field holds a reference to the Table object that represents
     * the SQL table used by the ${entity.name} entity.
     * ${entity.description!}
     */
    static final Table TABLE;

    <#list entity.allFields as field>
    /**
     * This static field holds a reference to the TableColumn object that represents
     * the SQL column used by the ${field.name} field.
     * ${field.description!}
     */
    static final ${field.columnClass}<${field.type.javaType}, ${field.type.readType}> ${field.column?upper_case};

    <#if field.class.simpleName == "RelationField">
    /**
     * This static field holds a reference to the TableColumn object that represents
     * the raw version of the SQL column used by the ${field.name} field.
     */
    static final ${field.columnClass}<${field.with.key.type.javaType}, ${field.with.key.type.readType}> ${field.column?upper_case}_KEY;

    </#if>
    </#list>
    static final Query FIND_QUERY;

    static final Query INSERT_QUERY;

    static final Query UPDATE_QUERY;

    static final Query DELETE_QUERY;

    static {
        RELATION_TYPE = SQL.buildType(<@compress single_line=true><#compress>
                            ${entity.name}.class, 
                            ${entity.key.type.readType}.class, 
                            JDBCType.${entity.key.type.jdbcType}, 
                            ${entity.key.type.length!0}, 
                            ${entity.key.type.precision!0}, 
                            null, 
                            <#if entity.key.type.writer??>
                            (e) -> ${entity.key.type.writerCode("e.get" + entity.key.name?cap_first + "()")}
                            <#else>
                            (e) -> e.get${entity.key.name?cap_first}()
                            </#if></#compress></@compress>);

        <#list entity.allFields as field>
        ${field.column?upper_case} = SQL.<#if field.autoIncrement>buildAiColumn<#else>build${field.columnClass}</#if>("${field.column}", ${field.fullTypeName}, ${field.required?string("false","true")}<#if !field.autoIncrement>, null</#if>);

        </#list>
        TABLE = SQL.buildTable("${entity.table?lower_case}")
                    .key(${entity.key.column?upper_case})
                    <#list entity.fields as field>
                    .column(${field.column?upper_case})
                    </#list>
                    <#list entity.indexes![] as index>
                    <#if index.unique && index.mustRemove>
                    .index(SQL.removeUnique(<#if index.name??>"${index.name}", </#if><#list index.fields![] as f>${f.column?upper_case}<#sep>, </#sep></#list>))
                    <#elseif index.mustRemove>
                    .index(SQL.removeIndex(<#if index.name??>"${index.name}", </#if><#list index.fields![] as f>${f.column?upper_case}<#sep>, </#sep></#list>))
                    <#elseif index.unique>
                    .index(SQL.buildUnique(<#if index.name??>"${index.name}", </#if><#list index.fields![] as f>${f.column?upper_case}<#sep>, </#sep></#list>))
                    <#else>
                    .index(SQL.buildIndex(<#if index.name??>"${index.name}", </#if><#list index.fields![] as f>${f.column?upper_case}<#sep>, </#sep></#list>))
                    </#if>
                    </#list>
                    <#list entity.foreignKeys![] as key>
                    <#if !key.isWithItSelf && !key.fkOnModel>
                    .foreignKey(SQL.buildForeignKey(${key.column?upper_case})
                                    .references(${key.with.name}_.TABLE)
                                    .strategy(ForeignKeyStrategy.${key.onUpdate}, ForeignKeyStrategy.${key.onDelete})
                                    .build())
                    </#if>
                    </#list>
                    .build();

        <#list entity.allFields as field>
        <#if field.class.simpleName == "RelationField">
        ${field.column?upper_case}_KEY = SQL.build${field.columnClass}("${field.column}", TABLE, ${field.with.key.fullTypeName}, ${field.required?string("false","true")}<#if !field.autoIncrement>, null</#if>);

        </#if>
        </#list>
        FIND_QUERY = SQL.select(<#list entity.allFields as field>${field.column?upper_case}<#sep>, </#sep></#list>)
                        .from(TABLE)
                        .where(${entity.key.column?upper_case}.eq(${entity.key.column?upper_case}.asParam()))
                        .toQuery();
        INSERT_QUERY = SQL.insertInto(TABLE)
                        .columns(<#list entity.nonAiFields as field>${field.column?upper_case}<#sep>, </#sep></#list>)
                        .values(<#list entity.nonAiFields as field>${field.column?upper_case}.asParam()<#sep>, </#sep></#list>)
                        .toQuery();
        UPDATE_QUERY = SQL.update(TABLE)
                        <#list entity.nonAiFields as field>
                        .set(${field.column?upper_case}, ${field.column?upper_case}.asParam())
                        </#list>
                        .where(${entity.key.column?upper_case}.eq(${entity.key.column?upper_case}.asParam()))
                        .toQuery();
        DELETE_QUERY = SQL.delete()
                        .from(TABLE)
                        .where(${entity.key.column?upper_case}.eq(${entity.key.column?upper_case}.asParam()))
                        .toQuery();
    }
}
