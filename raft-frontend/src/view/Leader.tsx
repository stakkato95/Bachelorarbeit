import React from 'react';

import { createStyles, Theme, makeStyles } from '@material-ui/core/styles';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemText from '@material-ui/core/ListItemText';
import ListItemAvatar from '@material-ui/core/ListItemAvatar';
import Avatar from '@material-ui/core/Avatar';
import ImageIcon from '@material-ui/icons/Image';
import WorkIcon from '@material-ui/icons/Work';
import BeachAccessIcon from '@material-ui/icons/BeachAccess';

import Typography from '@material-ui/core/Typography';

import Collapse from '@material-ui/core/Collapse';
import ExpandLess from '@material-ui/icons/ExpandLess';
import ExpandMore from '@material-ui/icons/ExpandMore';
import ListItemIcon from '@material-ui/core/ListItemIcon';
import StarBorder from '@material-ui/icons/StarBorder';

import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableContainer from '@material-ui/core/TableContainer';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';
import Paper from '@material-ui/core/Paper';

const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        root: {
            width: '100%',
            maxWidth: 360,
            backgroundColor: theme.palette.background.paper,
        },
        nested: {
            paddingLeft: theme.spacing(4),
        },
        table: {
            // minWidth: 650,
        },
    }),
);





function createData(name: string, calories: number) {
    return { name, calories };
}

const rows = [
    createData('Frozen yoghurt', 159),
    createData('Ice cream sandwich', 237),
    createData('Eclair', 262),
    createData('Cupcake', 305),
    createData('Gingerbread', 356),
];





export default function Leader(props: any) {
    const { leader } = props;
    const classes = useStyles();
    const [openNextIndices, setOpenNextIndices] = React.useState(false);
    const [openPendingItems, setOpenPendingItems] = React.useState(false);
    const handleClickNextIndices = () => {
        setOpenNextIndices(!openNextIndices);
    };
    const handleClickPendingItems = () => {
        setOpenPendingItems(!openPendingItems);
    };

    let leaderCommit = leader.leaderCommit === undefined ? "(no values commited yet)" : leader.leaderCommit

    console.log(leader)

    return (<>
        <Typography variant="h4" gutterBottom>Leader info</Typography>
        <List className={classes.root}>
            <ListItem>
                <ListItemAvatar>
                    <Avatar>
                        <ImageIcon />
                    </Avatar>
                </ListItemAvatar>
                <ListItemText primary={leader.nodeId} secondary="id" />
            </ListItem>
            <ListItem>
                <ListItemAvatar>
                    <Avatar>
                        <WorkIcon />
                    </Avatar>
                </ListItemAvatar>
                <ListItemText primary={leaderCommit} secondary="Leader commit" />
            </ListItem>
            {/*  */}
            <ListItem button onClick={handleClickNextIndices}>
                <ListItemAvatar>
                    <Avatar>
                        <BeachAccessIcon />
                    </Avatar>
                </ListItemAvatar>
                <ListItemText primary="Next indices"/>
                {openNextIndices ? <ExpandLess /> : <ExpandMore />}
            </ListItem>
            <Collapse in={openNextIndices} timeout="auto" unmountOnExit>
                <TableContainer component={Paper}>
                    <Table className={classes.table} aria-label="simple table">
                        <TableHead>
                            <TableRow>
                                <TableCell>Akka node name</TableCell>
                                <TableCell align="center">Next index</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {Object.keys(leader.nextIndices).map((itemKey, i) => (
                                <TableRow key={itemKey}>
                                    <TableCell component="th" scope="row">
                                        {itemKey}
                                    </TableCell>
                                    <TableCell align="center">{leader.nextIndices[itemKey]}</TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </TableContainer>
            </Collapse>
            {/*  */}
            <ListItem button onClick={handleClickPendingItems}>
                <ListItemAvatar>
                    <Avatar>
                        <BeachAccessIcon />
                    </Avatar>
                </ListItemAvatar>
                <ListItemText primary="Pending items"/>
                {openPendingItems ? <ExpandLess /> : <ExpandMore />}
            </ListItem>
            <Collapse in={openPendingItems} timeout="auto" unmountOnExit>
                <TableContainer component={Paper}>
                    <Table className={classes.table} aria-label="simple table">
                        <TableHead>
                            <TableRow>
                                <TableCell>Akka node name</TableCell>
                                <TableCell align="center">Next index</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {Object.keys(leader.nextIndices).map((itemKey, i) => (
                                <TableRow key={itemKey}>
                                    <TableCell component="th" scope="row">
                                        {itemKey}
                                    </TableCell>
                                    <TableCell align="center">{leader.nextIndices[itemKey]}</TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </TableContainer>
            </Collapse>
        </List>
    </>);
}