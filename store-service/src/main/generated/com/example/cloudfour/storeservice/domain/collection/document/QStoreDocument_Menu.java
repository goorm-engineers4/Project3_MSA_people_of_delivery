package com.example.cloudfour.storeservice.domain.collection.document;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QStoreDocument_Menu is a Querydsl query type for Menu
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QStoreDocument_Menu extends BeanPath<StoreDocument.Menu> {

    private static final long serialVersionUID = -116374392L;

    public static final QStoreDocument_Menu menu = new QStoreDocument_Menu("menu");

    public final StringPath content = createString("content");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final ComparablePath<java.util.UUID> id = createComparable("id", java.util.UUID.class);

    public final SimplePath<StoreDocument.MenuCategory> menuCategory = createSimple("menuCategory", StoreDocument.MenuCategory.class);

    public final ListPath<StoreDocument.MenuOption, SimplePath<StoreDocument.MenuOption>> menuOptions = this.<StoreDocument.MenuOption, SimplePath<StoreDocument.MenuOption>>createList("menuOptions", StoreDocument.MenuOption.class, SimplePath.class, PathInits.DIRECT2);

    public final StringPath menuPicture = createString("menuPicture");

    public final EnumPath<com.example.cloudfour.storeservice.domain.menu.enums.MenuStatus> menuStatus = createEnum("menuStatus", com.example.cloudfour.storeservice.domain.menu.enums.MenuStatus.class);

    public final StringPath name = createString("name");

    public final NumberPath<Integer> price = createNumber("price", Integer.class);

    public QStoreDocument_Menu(String variable) {
        super(StoreDocument.Menu.class, forVariable(variable));
    }

    public QStoreDocument_Menu(Path<? extends StoreDocument.Menu> path) {
        super(path.getType(), path.getMetadata());
    }

    public QStoreDocument_Menu(PathMetadata metadata) {
        super(StoreDocument.Menu.class, metadata);
    }

}

